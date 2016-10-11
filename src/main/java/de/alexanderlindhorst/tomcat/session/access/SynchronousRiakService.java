/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.access;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
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

import de.alexanderlindhorst.tomcat.session.manager.BackendServiceBase;

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
    private RiakClient client;

    @Override
    protected void persistSessionInternal(String sessionId, byte[] bytes) {
        if (isShuttingDown()) {
            throw new RiakAccessException("Service is shutting down", null);
        }
        LOGGER.debug("persistSessionInternal {}", sessionId);
        try {
            //look up in indices
            Location location = new Location(SESSIONS, sessionId);
            RiakObject riakObject = getRiakObjectForSessionId(location);
            if (riakObject == null) {
                riakObject = new RiakObject();
            } else {
                //remove from indices with old values
                riakObject.getIndexes().getIndex(LongIntIndex.named(LAST_ACCESSED)).removeAll();
            }

            //update object
            riakObject.setValue(BinaryValue.create(bytes));
            riakObject.getIndexes().getIndex(LongIntIndex.named(LAST_ACCESSED)).add(currentTimeMillis());

            //store updated object
            StoreValue storeOp = new StoreValue.Builder(riakObject)
                    .withLocation(location)
                    .build();
            StoreValue.Response response = client.execute(storeOp);
            LOGGER.debug("persistSessionInternal - Response: {}", response);
        } catch (ExecutionException | InterruptedException exception) {
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
            RiakObject value = getRiakObjectForSessionId(location);
            if (value == null) {
                return null;
            }
            return value.getValue().getValue();
        } catch (ExecutionException | InterruptedException ex) {
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
            client.execute(deleteValue);
        } catch (ExecutionException | InterruptedException ex) {
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
        List<String> expiredSessionIds = getExpiredSessionIds();
        while (!expiredSessionIds.isEmpty()) {
            expiredSessionIds.forEach(id -> {
                deleteSessionInternal(id);
                accumulated.add(id);
            });
            //don't wait if the last batch wasn't maxed up
            if (expiredSessionIds.size() == BATCH_SIZE) {
                try {
                    MILLISECONDS.sleep(500);
                } catch (InterruptedException ex) {
                    LOGGER.warn("Interruption occured while waiting before the next batch to be fetched", ex);
                }
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
            } catch (ExecutionException | InterruptedException ex) {
                LOGGER.error("Interruption while executing query", ex);
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
