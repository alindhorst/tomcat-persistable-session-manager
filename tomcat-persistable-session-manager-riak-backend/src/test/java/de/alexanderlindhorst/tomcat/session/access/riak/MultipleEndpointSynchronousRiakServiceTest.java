/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.access.riak;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.basho.riak.client.core.RiakCluster;

import static com.google.common.collect.Lists.newArrayList;
import static de.alexanderlindhorst.tomcat.session.manager.testutils.TestUtils.getFieldValueFromObject;
import static de.alexanderlindhorst.tomcat.session.manager.testutils.TestUtils.setFieldValueForObject;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author alindhorst
 */
@RunWith(MockitoJUnitRunner.class)
public class MultipleEndpointSynchronousRiakServiceTest {

    private MultipleEndpointSynchronousRiakService instance;
    @Mock
    private RiakCluster cluster;
    @Mock
    private SynchronousRiakService backend1;
    @Mock
    private SynchronousRiakService backend2;

    @Before
    public void setup() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
        instance = new MultipleEndpointSynchronousRiakService();
        setFieldValueForObject(instance, "endpointDelegates", newArrayList(backend1, backend2));
    }

    @Test
    public void numberOfBackendAddressesReflectedInListOfDelegates() throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        //override instance initialized in setupinstance = new MultipleEndpointSynchronousRiakService();
        String address = "riak1;riak2:8087;riak3";
        instance.setBackendAddress(address);
        instance.init();

        ArrayList<String> addresses = newArrayList(instance.getBackendAddress().split(";"));
        @SuppressWarnings("unchecked")
        List<SynchronousRiakService> delegates = (List<SynchronousRiakService>) getFieldValueFromObject(instance,
                "endpointDelegates");

        assertThat(delegates, is(not(nullValue())));
        assertThat(delegates.size(), is(3));
        delegates.forEach(delegate -> assertThat(addresses.contains(delegate.getBackendAddress()), is(true)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroBackendsThrowsException() {
        instance.init();
    }

    @Test
    public void persistSessionInternalWritesToAllDelegates() {
        String id = "id";
        byte[] bytes = new byte[0];

        instance.persistSessionInternal(id, bytes);

        verify(backend1).persistSessionInternal(id, bytes);
        verify(backend2).persistSessionInternal(id, bytes);
    }

    @Test
    public void deleteSessionInternalWritesToAllDelegates() {
        String id = "id";

        instance.deleteSessionInternal(id);

        verify(backend1).deleteSessionInternal(id);
        verify(backend2).deleteSessionInternal(id);
    }

    @Test
    public void getSessionInternalFetchesFromExactlyOneBackend() {
        String id = "id";
        InvocationCountingAnswer answer = new InvocationCountingAnswer();
        when(backend1.getSessionInternal(anyString())).thenAnswer(answer);
        when(backend2.getSessionInternal(anyString())).thenAnswer(answer);

        instance.getSessionInternal(id);

        assertThat(answer.getCounter(), is(1));
    }

    @Test
    public void removeExpiredSessionsRemovesAllExpiredSessionsFromAllNodes() {
        ArrayList<String> list1 = newArrayList("1", "2", "3");
        ArrayList<String> list2 = newArrayList("3", "4", "1");
        List<String> expected = newArrayList();
        expected.addAll(list1);
        expected.addAll(list2);
        when(backend1.removeExpiredSessions()).thenReturn(list1);
        when(backend2.removeExpiredSessions()).thenReturn(list2);

        List<String> allRemoved = instance.removeExpiredSessions();

        expected.forEach(id -> assertThat(allRemoved.contains(id), is(true)));
    }

    @Test
    public void getExpiredSessionIdsGetsFromAllNodes() {
        ArrayList<String> list1 = newArrayList("1", "2", "3");
        ArrayList<String> list2 = newArrayList("3", "4", "1");
        List<String> expected = newArrayList();
        expected.addAll(list1);
        expected.addAll(list2);
        when(backend1.getExpiredSessionIds()).thenReturn(list1);
        when(backend2.getExpiredSessionIds()).thenReturn(list2);

        List<String> allExpired = instance.getExpiredSessionIds();

        expected.forEach(id -> assertThat(allExpired.contains(id), is(true)));
    }

    private static class InvocationCountingAnswer implements Answer<byte[]> {

        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public byte[] answer(InvocationOnMock invocation) throws Throwable {
            counter.incrementAndGet();
            return new byte[0];
        }

        public int getCounter() {
            return counter.get();
        }
    }
}
