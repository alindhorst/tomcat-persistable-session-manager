/*
 *  LICENSE INFORMATION:
 */
package de.alexanderlindhorst.riak.session.access;

import java.util.Map;

import org.apache.catalina.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.alexanderlindhorst.riak.session.TestUtils;
import de.alexanderlindhorst.riak.session.manager.RiakSession;
import de.alexanderlindhorst.riak.session.manager.RiakSessionManager;

import static de.alexanderlindhorst.riak.session.TestUtils.getFieldValueFromObject;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author lindhrst
 */
@RunWith(MockitoJUnitRunner.class)
public class FakeRiakServiceTest {

    @Mock
    private RiakSessionManager manager;
    @Mock
    private Context context;
    private FakeRiakService instance;

    @Before
    public void setUp() {
        instance = new FakeRiakService();
        when(manager.getContext()).thenReturn(context);
        when(context.getApplicationLifecycleListeners()).thenReturn(new Object[0]);
        when(manager.getJvmRoute()).thenReturn(jvmRoute);
    }
    private final String jvmRoute = "myhost-1";

    @Test
    public void persistSessionStoresSessionInternallyUnderIDWithoutJVMRoute() throws NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        RiakSession session = new RiakSession(manager);
        String sessionId = "testSession";
        session.setId(sessionId);
        instance.persistSession(session);
        @SuppressWarnings("unchecked")
        Map<String, RiakSession> map = (Map<String, RiakSession>) TestUtils.getFieldValueFromObject(instance,
                "sessionStore");
        assertThat(map.values().size(), is(1));
        assertThat(map.keySet().size(), is(1));
        assertThat(map.keySet().contains(sessionId), is(true));
        assertThat(map.values().contains(session), is(true));
    }

    @Test
    public void persistSessionStoresSessionInternallyUnderInternalIdWithJVMRoute() throws NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        RiakSession session = new RiakSession(manager);
        String sessionId = "testSession";
        session.setId(sessionId + "." + jvmRoute);
        instance.persistSession(session);
        @SuppressWarnings("unchecked")
        Map<String, RiakSession> map = (Map<String, RiakSession>) TestUtils.getFieldValueFromObject(instance,
                "sessionStore");
        assertThat(map.values().size(), is(1));
        assertThat(map.keySet().size(), is(1));
        assertThat(map.keySet().contains(sessionId), is(true));
        assertThat(map.values().contains(session), is(true));
    }

    @Test
    public void persistSessionOverwritesExistingValueWithNewerValue() throws NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        RiakSession session1 = new RiakSession(manager);
        session1.setId("session.host1");
        RiakSession session2 = new RiakSession(manager);
        session2.setId("session.host2");
        //simulate creation from first hosts
        instance.persistSession(session1);
        //and then updates on other host
        instance.persistSession(session2);
        @SuppressWarnings("unchecked")
        Map<String, RiakSession> map = (Map<String, RiakSession>) TestUtils.getFieldValueFromObject(instance,
                "sessionStore");
        assertThat(map.values().size(), is(1));
        assertThat(map.keySet().size(), is(1));
        assertThat(map.keySet().contains("session"), is(true));
        assertThat(map.values().contains(session2), is(true));
    }

    @Test
    public void getSessionRetrievesSessionFromStorage() {
        RiakSession session = new RiakSession(manager);
        String sessionId = "session";
        session.setId(sessionId);
        instance.persistSession(session);
        assertThat(instance.getSession(sessionId), is(session));
    }

    @Test
    public void deleteSessionRemovesSessionFromPersistenceLayer() throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        RiakSession session = new RiakSession(manager);
        String sessionId = "session";
        session.setId(sessionId);
        instance.persistSession(session);

        instance.deleteSession(session);
        @SuppressWarnings("unchecked")
        Map<String, RiakSession> map = (Map<String, RiakSession>) getFieldValueFromObject(instance, "sessionStore");
        assertThat(map.values().isEmpty(), is(true));
        assertThat(map.keySet().isEmpty(), is(true));
    }
}
