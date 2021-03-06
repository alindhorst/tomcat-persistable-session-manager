/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.access.riak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.RiakCommand;
import com.basho.riak.client.api.commands.indexes.IntIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
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
import static de.alexanderlindhorst.tomcat.session.manager.testutils.TestUtils.getFieldValueFromObject;
import static de.alexanderlindhorst.tomcat.session.manager.testutils.TestUtils.setFieldValueForObject;
import static de.alexanderlindhorst.tomcat.session.access.BackendServiceBase.SESSIONS_NEVER_EXPIRE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lindhrst
 */
@RunWith(MockitoJUnitRunner.Silent.class)
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
    @SuppressWarnings("unchecked")
    public void persistSessionInternalRunsStoreCommandOnClusterForNewObject() throws InterruptedException, ExecutionException {
        @SuppressWarnings("unchecked")
        RiakFuture<StoreOperation.Response, Location> storeFuture = mock(RiakFuture.class);
        RiakFuture<FetchOperation.Response, Location> fetchFuture = mock(RiakFuture.class);
        StoreOperation.Response storeOperationResponse = mock(StoreOperation.Response.class);
        FetchOperation.Response fetchOperationResponse = mock(FetchOperation.Response.class);
        when(fetchOperationResponse.getObjectList()).thenReturn(emptyList());
        when(storeFuture.get()).thenReturn(storeOperationResponse);
        when(fetchFuture.get()).thenReturn(fetchOperationResponse);
        when(cluster.execute(any(FutureOperation.class))).thenAnswer(invocation -> {
            if (StoreOperation.class.equals(invocation.getArguments()[0].getClass())) {
                return storeFuture;
            }
            if (FetchOperation.class.equals(invocation.getArguments()[0].getClass())) {
                return fetchFuture;
            }
            return null;
        });
        service.persistSessionInternal("sessionId", bytes);

        verify(cluster, times(2)).execute(operationCaptor.capture());

        FutureOperation<?, ?, ?> operation = operationCaptor.getValue();
        assertThat(operation.getClass().getName(), is(StoreOperation.class.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void persistSessionInternalRunsStoreCommandOnClusterForExistingObject() throws InterruptedException, ExecutionException {
        @SuppressWarnings("unchecked")
        RiakFuture<StoreOperation.Response, Location> storeFuture = mock(RiakFuture.class);
        RiakFuture<FetchOperation.Response, Location> fetchFuture = mock(RiakFuture.class);
        StoreOperation.Response storeOperationResponse = mock(StoreOperation.Response.class);
        FetchOperation.Response fetchOperationResponse = mock(FetchOperation.Response.class);
        when(storeFuture.get()).thenReturn(storeOperationResponse);
        when(fetchFuture.get()).thenReturn(fetchOperationResponse);
        when(fetchOperationResponse.getObjectList()).thenReturn(Collections.singletonList(new RiakObject()));
        when(cluster.execute(any(FutureOperation.class))).thenAnswer(invocation -> {
            if (StoreOperation.class.equals(invocation.getArguments()[0].getClass())) {
                return storeFuture;
            }
            if (FetchOperation.class.equals(invocation.getArguments()[0].getClass())) {
                return fetchFuture;
            }
            return null;
        });
        service.persistSessionInternal("sessionId", bytes);

        verify(cluster, times(2)).execute(operationCaptor.capture());

        FutureOperation<?, ?, ?> operation = operationCaptor.getValue();
        assertThat(operation.getClass().getName(), is(StoreOperation.class.getName()));
    }

    @Test(expected = RiakAccessException.class)
    @SuppressWarnings("unchecked")
    public void executionExceptionWhilePersistingThrowsRiakAccessException() throws ExecutionException, InterruptedException {
        doThrow(ExecutionException.class).when(client).execute(any(RiakCommand.class));
        service.persistSessionInternal("any", bytes);
    }

    @Test(expected = RiakAccessException.class)
    @SuppressWarnings("unchecked")
    public void interruptedExceptionWhilePersistingThrowsRiakAccessException() throws ExecutionException, InterruptedException {
        doThrow(ExecutionException.class).when(client).execute(any(RiakCommand.class));
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
    public void executionExceptionWhileGettingSessionThrowsRiakAccessException() throws ExecutionException, InterruptedException {
        doThrow(ExecutionException.class).when(client).execute(any(RiakCommand.class));
        service.getSessionInternal("any");
    }

    @Test(expected = RiakAccessException.class)
    @SuppressWarnings("unchecked")
    public void interruptedExceptionWhileGettingSessionThrowsRiakAccessException() throws ExecutionException, InterruptedException {
        doThrow(InterruptedException.class).when(client).execute(any(RiakCommand.class));
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
    public void executionExceptionWhileDeletingSessionThrowsRiakAccessException() throws ExecutionException, InterruptedException {
        doThrow(ExecutionException.class).when(client).execute(any(RiakCommand.class));
        service.deleteSessionInternal("any");
    }

    @Test(expected = RiakAccessException.class)
    @SuppressWarnings("unchecked")
    public void interruptedExceptionWhileDeletingSessionThrowsRiakAccessException() throws ExecutionException, InterruptedException {
        doThrow(InterruptedException.class).when(client).execute(any(RiakCommand.class));
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
        QueryOverride.ResponseOverride response = new QueryOverride.ResponseOverride(expectedValues);
        doReturn(response).when(client).execute(any(IntIndexQuery.class));
        service.setSessionExpiryThreshold(30000);

        List<String> expiredSessionIds = service.getExpiredSessionIds();

        assertThat(expiredSessionIds, is(not(nullValue())));
        assertThat(expiredSessionIds.size(), is(3));
        expectedValues.forEach(item -> assertThat(expiredSessionIds.contains(item), is(true)));
    }

    @Test
    public void removeExpiredSessionOnBatchGreaterBatchSize() throws ExecutionException, InterruptedException {
        ArrayList<String> batch1 = newArrayList();
        for (int i = 0; i < 1000; i++) {
            batch1.add(Integer.toString(i));
        }
        ArrayList<String> batch2 = newArrayList();
        for (int i = 1000; i < 1500; i++) {
            batch2.add(Integer.toString(i));
        }
        ArrayList<String> batch3 = newArrayList();
        ArrayList<String> expectedValues = newArrayList();
        expectedValues.addAll(batch1);
        expectedValues.addAll(batch2);
        QueryOverride.ResponseOverride response1 = new QueryOverride.ResponseOverride(batch1);
        QueryOverride.ResponseOverride response2 = new QueryOverride.ResponseOverride(batch2);
        QueryOverride.ResponseOverride response3 = new QueryOverride.ResponseOverride(batch3);
        doAnswer(new MultipleIntIndexQueryResponseAnswer(response1, response2, response3)).when(client).execute(
                any(IntIndexQuery.class));
        service.setSessionExpiryThreshold(30000);
        doAnswer(new EmptyResponseAnswer()).when(client).execute(any(DeleteValue.class));

        List<String> expiredSessionIds = service.removeExpiredSessions();

        assertThat(expiredSessionIds, is(not(nullValue())));
        assertThat(expiredSessionIds.size(), is(1500));
        expectedValues.forEach(item -> assertThat(expiredSessionIds.contains(item), is(true)));
    }

    @Test
    public void removeExpiredSessionOnBatchOfBatchSize() throws ExecutionException, InterruptedException {
        ArrayList<String> batch1 = newArrayList();
        for (int i = 0; i < 1000; i++) {
            batch1.add(Integer.toString(i));
        }
        ArrayList<String> batch2 = newArrayList();
        ArrayList<String> expectedValues = newArrayList();
        expectedValues.addAll(batch1);
        expectedValues.addAll(batch2);
        QueryOverride.ResponseOverride response1 = new QueryOverride.ResponseOverride(batch1);
        QueryOverride.ResponseOverride response2 = new QueryOverride.ResponseOverride(batch2);
        doAnswer(new MultipleIntIndexQueryResponseAnswer(response1, response2)).when(client).execute(any(IntIndexQuery.class));
        service.setSessionExpiryThreshold(30000);
        doAnswer(new EmptyResponseAnswer()).when(client).execute(any(DeleteValue.class));

        List<String> expiredSessionIds = service.removeExpiredSessions();

        assertThat(expiredSessionIds, is(not(nullValue())));
        assertThat(expiredSessionIds.size(), is(1000));
        expectedValues.forEach(item -> assertThat(expiredSessionIds.contains(item), is(true)));
    }

    @Test
    public void removeExpiredSessionOnBatchSmallerBatchSize() throws ExecutionException, InterruptedException {
        ArrayList<String> batch1 = newArrayList();
        for (int i = 0; i < 999; i++) {
            batch1.add(Integer.toString(i));
        }
        ArrayList<String> batch2 = newArrayList();
        ArrayList<String> expectedValues = newArrayList();
        expectedValues.addAll(batch1);
        expectedValues.addAll(batch2);
        QueryOverride.ResponseOverride response1 = new QueryOverride.ResponseOverride(batch1);
        QueryOverride.ResponseOverride response2 = new QueryOverride.ResponseOverride(batch2);
        doAnswer(new MultipleIntIndexQueryResponseAnswer(response1, response2)).when(client).execute(any(IntIndexQuery.class));
        service.setSessionExpiryThreshold(30000);
        doAnswer(new EmptyResponseAnswer()).when(client).execute(any(DeleteValue.class));

        List<String> expiredSessionIds = service.removeExpiredSessions();

        assertThat(expiredSessionIds, is(not(nullValue())));
        assertThat(expiredSessionIds.size(), is(999));
        expectedValues.forEach(item -> assertThat(expiredSessionIds.contains(item), is(true)));
    }

    @Test
    public void removeExpiredSessionOnEmptyBatch() throws ExecutionException, InterruptedException {
        ArrayList<String> batch1 = newArrayList();
        ArrayList<String> expectedValues = newArrayList();
        expectedValues.addAll(batch1);
        QueryOverride.ResponseOverride response1 = new QueryOverride.ResponseOverride(batch1);
        doAnswer(new MultipleIntIndexQueryResponseAnswer(response1)).when(client).execute(any(IntIndexQuery.class));
        service.setSessionExpiryThreshold(30000);

        List<String> expiredSessionIds = service.removeExpiredSessions();

        assertThat(expiredSessionIds, is(not(nullValue())));
        assertThat(expiredSessionIds.isEmpty(), is(true));
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
        //set up an exception, so we can see it not getting fired (graceful shutdown)
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

    private class MultipleIntIndexQueryResponseAnswer implements Answer<QueryOverride.ResponseOverride> {

        private final List<QueryOverride.ResponseOverride> responses;
        private int index = 0;

        public MultipleIntIndexQueryResponseAnswer(QueryOverride.ResponseOverride... responses) {
            this.responses = newArrayList(responses);
        }

        @Override
        public QueryOverride.ResponseOverride answer(InvocationOnMock invocation) throws Throwable {
            if (!IntIndexQuery.class.equals(invocation.getArguments()[0].getClass())) {
                return null;
            }
            QueryOverride.ResponseOverride response = responses.get(index % responses.size());
            index++;
            return response;
        }

    }

    private class EmptyResponseAnswer implements Answer<RiakCommand<Void, Location>> {

        @Override
        public RiakCommand<Void, Location> answer(InvocationOnMock invocation) throws Throwable {
            return null;
        }

    }
}
