/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.access;

import de.alexanderlindhorst.tomcat.session.access.FakeRiakService;

import java.util.Arrays;
import java.util.Map;

import org.apache.catalina.Context;
import org.apache.juli.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.alexanderlindhorst.tomcat.session.TestUtils;
import de.alexanderlindhorst.tomcat.session.manager.PersistableSession;
import de.alexanderlindhorst.tomcat.session.manager.RiakSessionManager;

import static de.alexanderlindhorst.tomcat.session.TestUtils.getFieldValueFromObject;
import static de.alexanderlindhorst.tomcat.session.manager.PersistableSessionUtils.serializeSession;
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
    @Mock
    private Log logger;
    private FakeRiakService instance;

    @Before
    public void setUp() {
        instance = new FakeRiakService();
        when(manager.getContext()).thenReturn(context);
        when(context.getApplicationLifecycleListeners()).thenReturn(new Object[0]);
        when(context.getLogger()).thenReturn(logger);
        when(logger.isDebugEnabled()).thenReturn(Boolean.FALSE);
        when(manager.getJvmRoute()).thenReturn(jvmRoute);
    }
    private final String jvmRoute = "myhost-1";

    @Test
    public void persistSessionStoresSessionInternallyUnderIDWithoutJVMRoute() throws NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        PersistableSession session = new PersistableSession(manager);
        String sessionId = "testSession";
        session.setId(sessionId);
        instance.persistSession(session);
        @SuppressWarnings("unchecked")
        Map<String, PersistableSession> map = (Map<String, PersistableSession>) TestUtils.getFieldValueFromObject(
                instance,
                "sessionStore");
        assertThat(map.values().size(), is(1));
        assertThat(map.keySet().size(), is(1));
        assertThat(map.keySet().contains(sessionId), is(true));
    }

    @Test
    public void persistSessionStoresSessionInternallyUnderInternalIdWithJVMRoute() throws NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        PersistableSession session = new PersistableSession(manager);
        String sessionId = "testSession";
        session.setId(sessionId + "." + jvmRoute);
        instance.persistSession(session);
        @SuppressWarnings("unchecked")
        Map<String, PersistableSession> map = (Map<String, PersistableSession>) TestUtils.getFieldValueFromObject(
                instance,
                "sessionStore");
        assertThat(map.values().size(), is(1));
        assertThat(map.keySet().size(), is(1));
        assertThat(map.keySet().contains(sessionId), is(true));
    }

    @Test
    public void persistSessionOverwritesExistingValueWithNewerValue() throws NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        PersistableSession session1 = new PersistableSession(manager);
        session1.setId("session.host1");
        PersistableSession session2 = new PersistableSession(manager);
        session2.setId("session.host2");
        byte[] serialized = serializeSession(session2);
        //simulate creation from first hosts
        instance.persistSession(session1);
        //and then updates on other host
        instance.persistSession(session2);
        @SuppressWarnings("unchecked")
        Map<String, byte[]> map = (Map<String, byte[]>) TestUtils.getFieldValueFromObject(instance,
                "sessionStore");
        assertThat(map.values().size(), is(1));
        assertThat(map.keySet().size(), is(1));
        assertThat(map.keySet().contains("session"), is(true));
        assertThat(Arrays.equals(map.get("session"), serialized), is(true));
    }

    @Test
    public void persistSessionResetsDirtyFlag() {
        PersistableSession session = new PersistableSession(manager);
        session.setId("session");
        session.setDirty(true);
        instance.persistSession(session);
        assertThat(session.isDirty(), is(false));
    }

    @Test
    public void getSessionRetrievesSessionFromStorage() {
        PersistableSession session = new PersistableSession(manager);
        PersistableSession deserialized = new PersistableSession(manager);
        String sessionId = "session";
        session.setId(sessionId);
        instance.persistSession(session);
        assertThat(instance.getSession(deserialized, sessionId).getId(), is(session.getId()));
    }

    @Test
    public void deleteSessionRemovesSessionFromPersistenceLayer() throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        PersistableSession session = new PersistableSession(manager);
        String sessionId = "session";
        session.setId(sessionId);
        instance.persistSession(session);

        instance.deleteSession(session);
        @SuppressWarnings("unchecked")
        Map<String, PersistableSession> map = (Map<String, PersistableSession>) getFieldValueFromObject(instance,
                "sessionStore");
        assertThat(map.values().isEmpty(), is(true));
        assertThat(map.keySet().isEmpty(), is(true));
    }

    @Test
    public void initDoesNotThrowException() {
        instance.init();
    }
}
