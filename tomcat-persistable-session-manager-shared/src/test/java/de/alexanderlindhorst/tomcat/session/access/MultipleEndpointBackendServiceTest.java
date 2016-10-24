/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.access;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.alexanderlindhorst.tomcat.session.manager.PersistableSession;

import static com.google.common.collect.Lists.newArrayList;
import static de.alexanderlindhorst.tomcat.session.manager.testutils.TestUtils.getFieldValueFromObject;
import static de.alexanderlindhorst.tomcat.session.manager.testutils.TestUtils.setFieldValueForObject;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author alindhorst
 */
@RunWith(MockitoJUnitRunner.class)
public class MultipleEndpointBackendServiceTest {

    private MultipleEndpointBackendService<FakeBackendService> instance;
    @Mock
    PersistableSession session = mock(PersistableSession.class);
    @Mock
    private FakeBackendServiceTestable backend1;
    @Mock
    private FakeBackendServiceTestable backend2;

    @Before
    public void setup() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
        instance = new MultipleEndpointBackendService<>();
        setFieldValueForObject(instance, "endpointDelegates", newArrayList(backend1, backend2));
    }

    @Test
    public void numberOfBackendAddressesReflectedInListOfDelegates() throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        //override instance initialized in setupinstance = new MultipleEndpointBackendService();
        String address = "a;b;c";
        instance.setBackendServiceType(FakeBackendService.class.getCanonicalName());
        instance.setBackendAddress(address);
        instance.init();

        ArrayList<String> addresses = newArrayList(instance.getBackendAddress().split(";"));
        @SuppressWarnings("unchecked")
        List<FakeBackendService> delegates = (List<FakeBackendService>) getFieldValueFromObject(instance,
                "endpointDelegates");

        assertThat(delegates, is(not(nullValue())));
        assertThat(delegates.size(), is(3));
        delegates.forEach(delegate -> assertThat(addresses.contains(delegate.getBackendAddress()), is(true)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullBackendAddressThrowsException() {
        instance.setBackendServiceType(FakeBackendService.class.getCanonicalName());
        instance.init();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullBackendTypeThrowsException() {
        instance.setBackendAddress("something");
        instance.init();
    }

    @Test(expected = IllegalStateException.class)
    public void uninstantiableBackendTypeThrowsException() {
        instance.setBackendServiceType("java.lang.Integer");
        instance.setBackendAddress("something");
        instance.init();
    }

    @Test
    public void persistSessionWritesToAllDelegates() {
        instance.persistSession(session);

        verify(backend1).persistSession(session);
        verify(backend2).persistSession(session);
    }

    @Test
    public void deleteSessionWritesToAllDelegates() {
        instance.deleteSession(session);

        verify(backend1).deleteSession(session);
        verify(backend2).deleteSession(session);
    }

    @Test
    public void getSessionInternalFetchesFromExactlyOneBackend() {
        String id = "id";
        InvocationCountingAnswer answer = new InvocationCountingAnswer();
        when(backend1.getSessionInternal(anyString())).thenAnswer(answer);
        when(backend2.getSessionInternal(anyString())).thenAnswer(answer);

        instance.getSession(session, id);

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

    @Test(expected = IllegalArgumentException.class)
    public void unknownTypeLeadsToIllegalArgumentException() {
        instance.setBackendServiceType("not.a.known.type");
    }

    @Test
    public void sessionManagementLoggerCanBeRead() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Logger logger = LoggerFactory.getLogger("TestLogger");
        instance.setSessionManagementLogger(logger);
        Logger loggerFromInstance = (Logger) getFieldValueFromObject(instance, "sessionManagementLogger");

        assertThat(loggerFromInstance, is(logger));
    }

    @Test
    public void sessionExpiryThresholdCanBeRead() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        instance.setSessionExpiryThreshold(10l);
        long thresholdFromObject = (long) getFieldValueFromObject(instance, "sessionExpiryThreshold");

        assertThat(thresholdFromObject, is(10l));
    }

    @Test
    public void shutdownIsSetOnAllBackends() {
        ArrayList<FakeBackendServiceTestable> expected = newArrayList(backend1, backend2);

        instance.shutdown();

        expected.forEach(backend -> verify(backend).shutdown());
    }

    private class InvocationCountingAnswer implements Answer<byte[]> {

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

    private static class FakeBackendServiceTestable extends FakeBackendService {

        @Override
        protected byte[] getSessionInternal(String sessionId) {
            return super.getSessionInternal(sessionId);
        }

    }
}
