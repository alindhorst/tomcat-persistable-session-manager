/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.indexes.IntIndexQuery;
import com.basho.riak.client.core.FutureOperation;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.RiakNode;
import com.basho.riak.client.core.operations.DeleteOperation;
import com.basho.riak.client.core.operations.FetchOperation;
import com.basho.riak.client.core.operations.StoreOperation;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.RiakObject;
import com.basho.riak.client.core.util.BinaryValue;

import static com.google.common.collect.Lists.newArrayList;
import static de.alexanderlindhorst.tomcat.session.TestUtils.getFieldValueFromObject;
import static de.alexanderlindhorst.tomcat.session.TestUtils.setFieldValueForObject;
import static de.alexanderlindhorst.tomcat.session.manager.BackendServiceBase.SESSIONS_NEVER_EXPIRE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lindhrst
 */
@RunWith(MockitoJUnitRunner.class)
public class SynchronousRiakServiceTest {

    @Mock
    private RiakCluster cluster;
    @Mock
    private Future<Boolean> shutdownFuture;
    @Captor
    private ArgumentCaptor<FutureOperation<?, ?, ?>> operationCaptor;
    private RiakClient client;
    private SynchronousRiakService service;
    private final byte[] bytes = new byte[]{1};

    @Before
    public void setup() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            InterruptedException, ExecutionException, TimeoutException {
        client = spy(new RiakClient(cluster));
        service = new SynchronousRiakService();
        setFieldValueForObject(service, "client", client);
        when(client.shutdown()).thenReturn(shutdownFuture);
        when(shutdownFuture.get(3, SECONDS)).thenReturn(TRUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void initFailsWithNullBackendAddress() {
        service.init();
    }

    @Test(expected = IllegalArgumentException.class)
    public void initFailsWithEmtpyBackendAddress() {
        service.setBackendAddress("");
        service.init();
    }

    @Test(expected = IllegalArgumentException.class)
    public void initFailsWithBackendAddressEndingInColon() {
        service.setBackendAddress("riak:");
        service.init();
    }

    @Test(expected = IllegalArgumentException.class)
    public void initFailsWithNonDigitPortBackendAddress() {
        service.setBackendAddress("riak:xyz");
        service.init();
    }

    @Test
    public void initProvidesCluster() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        service.setBackendAddress("riak");
        service.init();

        RiakCluster returnedCluster = ((RiakClient) getFieldValueFromObject(service, "client")).getRiakCluster();
        assertThat(returnedCluster, is(not(nullValue())));
        assertThat(returnedCluster.getNodes().size(), is(1));
    }

    @Test
    public void initSucceedsWithHostOnlyBackendAddress() throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        service.setBackendAddress("riak");
        service.init();

        RiakNode node = ((RiakClient) getFieldValueFromObject(service, "client")).getRiakCluster().getNodes().get(0);
        assertThat(node.getRemoteAddress(), is("riak"));
        assertThat(node.getPort(), is(10017));
    }

    @Test
    public void initSucceedsWithHostAndPortBackendAddress() throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        service.setBackendAddress("riak:100");
        service.init();

        RiakNode node = ((RiakClient) getFieldValueFromObject(service, "client")).getRiakCluster().getNodes().get(0);
        assertThat(node.getRemoteAddress(), is("riak"));
        assertThat(node.getPort(), is(100));
    }

    @Test
    public void persistSessionInternalRunsStoreCommandOnCluster() throws InterruptedException, ExecutionException {
        @SuppressWarnings("unchecked")
        RiakFuture<StoreOperation.Response, Location> coreFuture = mock(RiakFuture.class);
        StoreOperation.Response storeOperationResponse = mock(StoreOperation.Response.class);
        when(coreFuture.get()).thenReturn(storeOperationResponse);
        when(cluster.execute(any(StoreOperation.class))).thenReturn(coreFuture);
        service.persistSessionInternal("sessionId", bytes);

        verify(cluster).execute(operationCaptor.capture());

        FutureOperation<?, ?, ?> operation = operationCaptor.getValue();
        assertThat(operation.getClass().getName(), is(StoreOperation.class.getName()));
    }

    @Test(expected = RiakAccessException.class)
    @SuppressWarnings("unchecked")
    public void executionExceptionWhilePersistingThrowsRiakAccessException() {
        when(cluster.execute(any(FutureOperation.class))).thenThrow(ExecutionException.class);
        service.persistSessionInternal("any", bytes);
    }

    @Test(expected = RiakAccessException.class)
    @SuppressWarnings("unchecked")
    public void interruptedExceptionWhilePersistingThrowsRiakAccessException() {
        when(cluster.execute(any(FutureOperation.class))).thenThrow(InterruptedException.class);
        service.persistSessionInternal("any", bytes);
    }

    @Test
    public void getSessionInternalRunsFetchCommandOnCluster() throws InterruptedException, ExecutionException {
        RiakObject toReturn = new RiakObject();
        BinaryValue value = BinaryValue.create(bytes);
        toReturn.setValue(value);
        @SuppressWarnings("unchecked")
        RiakFuture<FetchOperation.Response, Location> coreFuture = mock(RiakFuture.class);
        FetchOperation.Response fetchOperationResponse = mock(FetchOperation.Response.class);
        when(fetchOperationResponse.getObjectList()).thenReturn(newArrayList(toReturn));
        when(coreFuture.get()).thenReturn(fetchOperationResponse);
        when(cluster.execute(any(FetchOperation.class))).thenReturn(coreFuture);
        byte[] serialized = service.getSessionInternal("sessionId");

        verify(cluster).execute(operationCaptor.capture());
        FutureOperation<?, ?, ?> operation = operationCaptor.getValue();
        assertThat(operation.getClass().getName(), is(FetchOperation.class.getName()));
        assertThat(Arrays.equals(serialized, value.getValue()), is(true));
    }

    @Test
    public void nullReturnValueFromServerWhileGettingSessionReturnsNull() throws InterruptedException,
            ExecutionException {
        RiakObject toReturn = new RiakObject();
        BinaryValue value = BinaryValue.create(bytes);
        toReturn.setValue(value);
        @SuppressWarnings("unchecked")
        RiakFuture<FetchOperation.Response, Location> coreFuture = mock(RiakFuture.class);
        FetchOperation.Response fetchOperationResponse = mock(FetchOperation.Response.class);
        when(fetchOperationResponse.getObjectList()).thenReturn(emptyList());
        when(coreFuture.get()).thenReturn(fetchOperationResponse);
        when(cluster.execute(any(FetchOperation.class))).thenReturn(coreFuture);

        byte[] serialized = service.getSessionInternal("sessionId");

        assertThat(serialized, is(nullValue()));
    }

    @Test(expected = RiakAccessException.class)
    @SuppressWarnings("unchecked")
    public void executionExceptionWhileGettingSessionThrowsRiakAccessException() {
        when(cluster.execute(any(FutureOperation.class))).thenThrow(ExecutionException.class);
        service.getSessionInternal("any");
    }

    @Test(expected = RiakAccessException.class)
    @SuppressWarnings("unchecked")
    public void interruptedExceptionWhileGettingSessionThrowsRiakAccessException() {
        when(cluster.execute(any(FutureOperation.class))).thenThrow(InterruptedException.class);
        service.getSessionInternal("any");
    }

    @Test
    public void deleteSessionInternalRunsDeleteCommandOnCluster() throws InterruptedException, ExecutionException {
        @SuppressWarnings("unchecked")
        RiakFuture<Void, Location> coreFuture = mock(RiakFuture.class);
        when(cluster.execute(any(DeleteOperation.class))).thenReturn(coreFuture);
        service.deleteSessionInternal("sessionId");

        verify(cluster).execute(operationCaptor.capture());
        FutureOperation<?, ?, ?> operation = operationCaptor.getValue();
        assertThat(operation.getClass().getName(), is(DeleteOperation.class.getName()));
    }

    @Test(expected = RiakAccessException.class)
    @SuppressWarnings("unchecked")
    public void executionExceptionWhileDeletingSessionThrowsRiakAccessException() {
        when(cluster.execute(any(FutureOperation.class))).thenThrow(ExecutionException.class);
        service.deleteSessionInternal("any");
    }

    @Test(expected = RiakAccessException.class)
    @SuppressWarnings("unchecked")
    public void interruptedExceptionWhileDeletingSessionThrowsRiakAccessException() {
        when(cluster.execute(any(FutureOperation.class))).thenThrow(InterruptedException.class);
        service.deleteSessionInternal("any");
    }

    @Test(expected = RiakAccessException.class)
    public void serviceShutDownMakeSessionPersistingFail() {
        service.shutdown();
        service.persistSessionInternal("sessionId", bytes);
    }

    @Test(expected = RiakAccessException.class)
    public void serviceShutDownMakeSessionGettingFail() {
        service.shutdown();
        service.getSessionInternal("sessionId");
    }

    @Test(expected = RiakAccessException.class)
    public void serviceShutDownMakeSessionDeletionFail() {
        service.shutdown();
        service.deleteSessionInternal("sessionId");
    }

    @Test
    public void getCleanUpRunIntervalSecondsGetSetValue() {
        service.setCleanUpRunIntervalSeconds(15);
        assertThat(service.getCleanUpRunIntervalSeconds(), is(15l));
    }

    @Test
    public void getSessionManagementLoggerReturnsSetValue() {
        Logger logger = LoggerFactory.getLogger("blub");
        service.setSessionManagementLogger(logger);

        assertThat(service.getSessionManagementLogger().getName(), is("blub"));
    }

    @Test
    public void getExpiredSessionIdsWontFetchAndGivesEmptySetForSESSION_NEVER_EXPIRES() {
        service.setSessionExpiryThreshold(SESSIONS_NEVER_EXPIRE);

        List<String> expiredSessionIds = service.getExpiredSessionIds();

        assertThat(expiredSessionIds.isEmpty(), is(true));
        verify(cluster, Mockito.never()).execute(any());
    }

    @Test
    public void getExpiredSessionIdsFetchesExpiredSessions() throws ExecutionException, InterruptedException {
        ArrayList<String> expectedValues = newArrayList("id1", "md5sum1919879", "nonsense");
        QueryOverride.ResponseOverride response = new QueryOverride.ResponseOverride(newArrayList("id1", "md5sum1919879", "nonsense"));
        doReturn(response).when(client).execute(any(IntIndexQuery.class));
        service.setSessionExpiryThreshold(30000);

        List<String> expiredSessionIds = service.getExpiredSessionIds();

        assertThat(expiredSessionIds, is(not(nullValue())));
        assertThat(expiredSessionIds.size(), is(3));
        expectedValues.forEach(item -> assertThat(expiredSessionIds.contains(item), is(true)));
    }

    @Test
    public void getExpiredSessionIdsGracefullyHandlesInterruptedException() throws ExecutionException, InterruptedException {
        doThrow(new InterruptedException()).when(client).execute((any(IntIndexQuery.class)));
        service.setSessionExpiryThreshold(30000);

        List<String> expiredSessionIds = service.getExpiredSessionIds();

        assertThat(expiredSessionIds.isEmpty(), is(true));
    }

    @Test
    public void getExpiredSessionIdsGracefullyHandlesExecutionException() throws ExecutionException, InterruptedException {
        doThrow(new ExecutionException(new RuntimeException("just for the hell of it"))).
                when(client).execute((any(IntIndexQuery.class)));
        service.setSessionExpiryThreshold(30000);

        List<String> expiredSessionIds = service.getExpiredSessionIds();

        assertThat(expiredSessionIds.isEmpty(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void serviceShutDownGracefullyHandlesException() throws InterruptedException, ExecutionException,
            TimeoutException {
        //override previously initialized object
        shutdownFuture = mock(Future.class);
        when(shutdownFuture.get(any(Long.class), any(TimeUnit.class))).thenThrow(new InterruptedException());
        service.shutdown();
    }

    private static class QueryOverride extends IntIndexQuery {

        private QueryOverride(
                Init<Long, ?> builder) {
            super(builder);
        }

        private static class ResponseOverride extends IntIndexQuery.Response {

            private final List<Entry> entries;

            private ResponseOverride(List<String> returnValues) {
                super(null, null, null);
                Namespace ns = new Namespace("SESSIONS");
                entries = returnValues.stream()
                        .map(id -> new EntryOverride(new Location(ns, BinaryValue.create(id)), BinaryValue.create(id)))
                        .collect(Collectors.toList());
            }

            @Override
            public List<Entry> getEntries() {
                return entries;
            }

            private class EntryOverride extends Entry {

                public EntryOverride(Location riakObjectLocation, BinaryValue indexKey) {
                    super(riakObjectLocation, indexKey, null);
                }
            }
        }
    }
}
