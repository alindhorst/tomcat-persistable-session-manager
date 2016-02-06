/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.access;

import java.util.concurrent.ExecutionException;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.basho.riak.client.core.FutureOperation;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.RiakNode;
import com.basho.riak.client.core.operations.FetchOperation;
import com.basho.riak.client.core.operations.StoreOperation;
import com.basho.riak.client.core.query.Location;

import de.alexanderlindhorst.riak.session.manager.PersistableSession;

import static de.alexanderlindhorst.riak.session.TestUtils.getFieldValueFromObject;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
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
    private PersistableSession session;
    @InjectMocks
    private SynchronousRiakService service;
    @Captor
    private ArgumentCaptor<FutureOperation<?, ?, ?>> operationCaptor;
    private final byte[] bytes = new byte[]{1};

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

        RiakCluster cluster = (RiakCluster) getFieldValueFromObject(service, "cluster");
        assertThat(cluster, is(not(nullValue())));
        assertThat(cluster.getNodes().size(), is(1));
    }

    @Test
    public void initSucceedsWithHostOnlyBackendAddress() throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        service.setBackendAddress("riak");
        service.init();

        RiakNode node = ((RiakCluster) getFieldValueFromObject(service, "cluster")).getNodes().get(0);
        assertThat(node.getRemoteAddress(), is("riak"));
        assertThat(node.getPort(), is(10017));
    }

    @Test
    public void initSucceedsWithHostAndPortBackendAddress() throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        service.setBackendAddress("riak:100");
        service.init();

        RiakNode node = ((RiakCluster) getFieldValueFromObject(service, "cluster")).getNodes().get(0);
        assertThat(node.getRemoteAddress(), is("riak"));
        assertThat(node.getPort(), is(100));
    }

    @Test
    public void persistSessionInternalRunsStoreCommandOnCluster() throws InterruptedException, ExecutionException {
        @SuppressWarnings("unchecked")
        RiakFuture<StoreOperation.Response, Location> coreFuture = mock(RiakFuture.class
        );
        StoreOperation.Response response = mock(StoreOperation.Response.class
        );
        when(coreFuture.get()).thenReturn(response);
        when(cluster.execute(any(StoreOperation.class
        ))).thenReturn(coreFuture);
        service.persistSessionInternal("sessionId", bytes);

        verify(cluster).execute(operationCaptor.capture());

        FutureOperation<?, ?, ?> operation = operationCaptor.getValue();
        assertThat(operation.getClass().getName(), is(StoreOperation.class
                .getName()));
    }

    @Test(expected = RiakAccessException.class)
    @SuppressWarnings("unchecked")
    public void executionExceptionWhilePersistingThrowsRiakAccessException() {
        when(cluster.execute(any(FutureOperation.class
        ))).thenThrow(ExecutionException.class
        );
        service.persistSessionInternal("any", bytes);
    }

    @Test(expected = RiakAccessException.class)
    @SuppressWarnings("unchecked")
    public void interruptedExceptionWhilePersistingThrowsRiakAccessException() {
        when(cluster.execute(any(FutureOperation.class
        ))).thenThrow(InterruptedException.class
        );
        service.persistSessionInternal("any", bytes);
    }

    @Test
    @Ignore
    public void getSessionInternalRunsFetchCommandOnCluster() {
        @SuppressWarnings("unchecked")
        RiakFuture<FetchOperation.Response, Location> coreFuture = mock(RiakFuture.class
        );
        when(cluster.execute(any(FetchOperation.class
        ))).thenReturn(coreFuture);
        service.getSessionInternal("sessionId");

        verify(cluster).execute(operationCaptor.capture());

        FutureOperation<?, ?, ?> operation = operationCaptor.getValue();
        assertThat(operation.getClass().getName(), is(FetchOperation.class
                .getName()));
    }
}
