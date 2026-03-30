/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.access.riak;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.indexes.IntIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.RiakObject;
import com.basho.riak.client.core.query.indexes.LongIntIndex;
import com.basho.riak.client.core.util.BinaryValue;
import com.google.common.collect.Lists;

import de.alexanderlindhorst.tomcat.session.access.BackendServiceBase;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

/**
 * @author alindhorst
 */
public class SynchronousRiakService extends BackendServiceBase {

    private static final Namespace SESSIONS = new Namespace("SESSIONS");
    private static final int BATCH_SIZE = 1000;
    private static final String LAST_ACCESSED = "_lastAccessed";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MS = 100;
    private RiakClient client;

    @FunctionalInterface
    private interface RiakOperation<T> {
        T execute() throws ExecutionException, InterruptedException;
    }

    private <T> T executeWithRetry(RiakOperation<T> operation) throws ExecutionException, InterruptedException {
        ExecutionException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return operation.execute();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (ExecutionException e) {
                lastException = e;
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    LOGGER.warn("Riak operation failed on attempt {}/{}, retrying", attempt, MAX_RETRY_ATTEMPTS, e);
                    MILLISECONDS.sleep(RETRY_BACKOFF_MS);
                }
            }
        }
        throw lastException;
    }

    @Override
    protected void persistSessionInternal(String sessionId, byte[] bytes) {
        if (isShuttingDown()) {
            throw new RiakAccessException("Service is shutting down", null);
        }
        LOGGER.debug("persistSessionInternal {}", sessionId);
        try {
            executeWithRetry(() -> {
                Location location = new Location(SESSIONS, sessionId);
                RiakObject existing = getRiakObjectForSessionId(location);
                RiakObject riakObject = (existing != null) ? existing : new RiakObject();
                if (existing != null) {
                    riakObject.getIndexes().getIndex(LongIntIndex.named(LAST_ACCESSED)).removeAll();
                }
                riakObject.setValue(BinaryValue.create(bytes));
                riakObject.getIndexes().getIndex(LongIntIndex.named(LAST_ACCESSED)).add(currentTimeMillis());
                StoreValue storeOp = new StoreValue.Builder(riakObject).withLocation(location).build();
                StoreValue.Response response = client.execute(storeOp);
                LOGGER.debug("persistSessionInternal - Response: {}", response);
                return null;
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RiakAccessException("Interrupted while persisting session", e);
        } catch (ExecutionException exception) {
            throw new RiakAccessException("Couldn't persist session", exception);
        }
    }

    @Override
    protected byte[] getSessionInternal(String sessionId) {
        if (isShuttingDown()) {
            throw new RiakAccessException("Service is shutting down", null);
        }
        try {
            LOGGER.debug("getSessionInternal {}", sessionId);
            Location location = new Location(SESSIONS, sessionId);
            return executeWithRetry(() -> {
                RiakObject value = getRiakObjectForSessionId(location);
                return (value == null) ? null : value.getValue().getValue();
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RiakAccessException("Interrupted while fetching session " + sessionId, e);
        } catch (ExecutionException ex) {
            throw new RiakAccessException("Couldn't fetch session " + sessionId, ex);
        }
    }

    @Override
    protected void deleteSessionInternal(String sessionId) {
        if (isShuttingDown()) {
            throw new RiakAccessException("Service is shutting down", null);
        }
        try {
            LOGGER.debug("deleteSessionInternal {}", sessionId);
            Location location = new Location(SESSIONS, sessionId);
            DeleteValue deleteValue = new DeleteValue.Builder(location).build();
            executeWithRetry(() -> { client.execute(deleteValue); return null; });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RiakAccessException("Interrupted while deleting session " + sessionId, e);
        } catch (ExecutionException ex) {
            throw new RiakAccessException("Couldn't delete session " + sessionId, ex);
        }
    }

    @Override
    public void init() {
        super.init();
        if (isNullOrEmpty(getBackendAddress())) {
            throw new IllegalArgumentException("backend address must not be null or empty");
        }
        Pattern addressPattern = Pattern.compile("^(?<host>[^:]+)(:(?<port>\\d+))?");
        Matcher matcher = addressPattern.matcher(getBackendAddress());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("backend address value " + getBackendAddress() + " cannot be read");
        }
        String host = matcher.group("host");
        String portValue = matcher.group("port");
        int port = 10017;
        if (!isNullOrEmpty(portValue)) {
            port = Integer.valueOf(portValue);
        }
        try {
            RiakNode node = new RiakNode.Builder()
                    .withRemoteAddress(host)
                    .withRemotePort(port)
                    .build();
            RiakCluster cluster = new RiakCluster.Builder(node)
                    .build();
            cluster.start();
            client = new RiakClient(cluster);
        } catch (UnknownHostException ex) {
            throw new IllegalStateException("Couldn't configure riak access", ex);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Future<Boolean> shutdown = client.shutdown();
        try {
            shutdown.get(3, SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            LOGGER.warn("Problem occured during Riak cluster shutdown", ex);
        }
    }

    /**
     * {@inheritDoc} This implementation fetches works in batches of 1000 ids. Between the batches there will be a pause of half a
     * second.
     */
    @Override
    public List<String> removeExpiredSessions() {
        List<String> accumulated = Lists.newArrayList();
        Set<String> processedIds = new HashSet<>();
        List<String> expiredSessionIds = getExpiredSessionIds();
        while (!expiredSessionIds.isEmpty()) {
            boolean anyNew = false;
            for (String id : expiredSessionIds) {
                if (processedIds.add(id)) {
                    deleteSessionInternal(id);
                    accumulated.add(id);
                    anyNew = true;
                }
            }
            if (!anyNew) {
                break;
            }
            expiredSessionIds = getExpiredSessionIds();
        }
        return accumulated;
    }

    @Override
    public List<String> getExpiredSessionIds() {
        if (getSessionExpiryThreshold() != -1) {
            try {
                long threshold = currentTimeMillis() - getSessionExpiryThreshold();
                IntIndexQuery.Response response = getLastAccessedQueryResults(0l, threshold);
                return response.getEntries().stream()
                        .map(entry -> entry.getRiakObjectLocation().getKeyAsString())
                        .collect(toList());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RiakAccessException("Interrupted while querying expired sessions", ex);
            } catch (ExecutionException ex) {
                throw new RiakAccessException("Failed to query expired sessions", ex);
            }
        }
        return Collections.<String>emptyList();
    }

    private IntIndexQuery.Response getLastAccessedQueryResults(long fromValue, long toValue) throws
            ExecutionException,
            InterruptedException {
        return client.execute(new IntIndexQuery.Builder(SESSIONS, LAST_ACCESSED, fromValue, toValue).withMaxResults(BATCH_SIZE).
                withPaginationSort(true).build());
    }

    private RiakObject getRiakObjectForSessionId(Location location) throws ExecutionException, InterruptedException {
        FetchValue fetchValue = new FetchValue.Builder(location).withOption(FetchValue.Option.DELETED_VCLOCK, true).build();
        RiakObject value = client.execute(fetchValue).getValue(RiakObject.class);
        return value;
    }
}
