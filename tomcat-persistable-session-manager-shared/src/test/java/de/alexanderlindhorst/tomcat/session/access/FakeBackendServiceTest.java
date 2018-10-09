/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.access;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.catalina.Context;
import org.apache.juli.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.alexanderlindhorst.tomcat.session.manager.PersistableSession;
import de.alexanderlindhorst.tomcat.session.manager.PersistableSessionManager;
import de.alexanderlindhorst.tomcat.session.manager.testutils.TestUtils;

import static com.google.common.collect.Maps.newHashMap;
import static de.alexanderlindhorst.tomcat.session.access.BackendServiceBase.SESSIONS_NEVER_EXPIRE;
import static de.alexanderlindhorst.tomcat.session.manager.PersistableSessionUtils.serializeSession;
import static de.alexanderlindhorst.tomcat.session.manager.testutils.TestUtils.getFieldValueFromObject;
import static de.alexanderlindhorst.tomcat.session.manager.testutils.TestUtils.setFieldValueForObject;
import static java.lang.System.currentTimeMillis;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author lindhrst
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class FakeBackendServiceTest {

    @Mock
    private PersistableSessionManager manager;
    @Mock
    private Context context;
    @Mock
    private Log logger;
    private FakeBackendService instance;

    @Before
    public void setUp() {
        instance = new FakeBackendService();
        when(manager.getContext()).thenReturn(context);
        when(context.getApplicationLifecycleListeners()).thenReturn(new Object[0]);
        when(context.getLogger()).thenReturn(logger);
        when(logger.isDebugEnabled()).thenReturn(Boolean.FALSE);
        when(manager.getJvmRoute()).thenReturn(jvmRoute);
    }

    @After
    public void tearDown() {
        //mimick regular shutdown
        instance.shutdown();
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

    @Test
    public void getExpiredSessionIdsReturnsEmptyListForSESSIONS_NEVER_EXPIRE() {
        List<String> expiredSessionIds = instance.getExpiredSessionIds();
        assertThat(expiredSessionIds.isEmpty(), is(true));
    }

    @Test
    public void getExpiredSessionIdsReturnsExpiredSessionIDs() throws IllegalArgumentException, IllegalAccessException,
            NoSuchFieldException {
        Map<String, byte[]> sessionStore = newHashMap();
        sessionStore.put("1", new byte[0]);
        sessionStore.put("2", new byte[0]);
        sessionStore.put("3", new byte[0]);
        setFieldValueForObject(instance, "sessionStore", sessionStore);
        Map<String, Long> lastAccessed = newHashMap();
        lastAccessed.put("1", currentTimeMillis());
        lastAccessed.put("2", currentTimeMillis() - 10000);
        lastAccessed.put("3", currentTimeMillis() - 100000);
        setFieldValueForObject(instance, "lastAccessed", lastAccessed);
        instance.setSessionExpiryThreshold(30000);

        List<String> expiredSessionIds = instance.getExpiredSessionIds();

        assertThat(expiredSessionIds.size(), is(1));
        assertThat(expiredSessionIds.get(0), is("3"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void removeExpiredSessionsRemovesNothingForSESSIONS_NEVER_EXPIRE() throws IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException {
        Map<String, byte[]> sessionStore = newHashMap();
        sessionStore.put("1", new byte[0]);
        sessionStore.put("2", new byte[0]);
        sessionStore.put("3", new byte[0]);
        setFieldValueForObject(instance, "sessionStore", sessionStore);
        Map<String, Long> lastAccessed = newHashMap();
        lastAccessed.put("1", currentTimeMillis());
        lastAccessed.put("2", currentTimeMillis() - 10000);
        lastAccessed.put("3", currentTimeMillis() - 100000);
        setFieldValueForObject(instance, "lastAccessed", lastAccessed);
        instance.setSessionExpiryThreshold(SESSIONS_NEVER_EXPIRE);

        List<String> removedSessions = instance.removeExpiredSessions();
        Map<String, byte[]> sessionStoreReadBack = (Map<String, byte[]>) getFieldValueFromObject(instance,
                "sessionStore");
        Map<String, Long> lastAccessReadBack = (Map<String, Long>) getFieldValueFromObject(instance, "lastAccessed");

        assertThat(removedSessions.isEmpty(), is(true));
        assertThat(sessionStoreReadBack.size(), is(3));
        assertThat(lastAccessReadBack.size(), is(3));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void removeExpiredSessionsRemovesExpiredSessionsOnly() throws IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException {
        Map<String, byte[]> sessionStore = newHashMap();
        sessionStore.put("1", new byte[0]);
        sessionStore.put("2", new byte[0]);
        sessionStore.put("3", new byte[0]);
        setFieldValueForObject(instance, "sessionStore", sessionStore);
        Map<String, Long> lastAccessed = newHashMap();
        lastAccessed.put("1", currentTimeMillis());
        lastAccessed.put("2", currentTimeMillis() - 10000);
        lastAccessed.put("3", currentTimeMillis() - 100000);
        setFieldValueForObject(instance, "lastAccessed", lastAccessed);
        instance.setSessionExpiryThreshold(30000);

        List<String> removedSessions = instance.removeExpiredSessions();
        Map<String, byte[]> sessionStoreReadBack = (Map<String, byte[]>) getFieldValueFromObject(instance,
                "sessionStore");
        Map<String, Long> lastAccessReadBack = (Map<String, Long>) getFieldValueFromObject(instance, "lastAccessed");

        assertThat(removedSessions.size(), is(1));
        assertThat(removedSessions.get(0), is("3"));
        assertThat(sessionStoreReadBack.size(), is(2));
        assertThat(lastAccessReadBack.size(), is(2));
    }

    @Test
    public void getSessionManagementLoggerReturnsSetLogger() {
        Logger testLogger = LoggerFactory.getLogger("TestLogger");
        instance.setSessionManagementLogger(testLogger);

        assertThat(instance.getSessionManagementLogger(), is(testLogger));
    }

    @Test
    public void shutdownCorrectlySignaled() {
        instance.shutdown();
        assertThat(instance.isShuttingDown(), is(true));
    }
}
