/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.manager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;

import org.apache.catalina.*;
import org.apache.catalina.session.StandardSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.alexanderlindhorst.tomcat.session.TestUtils.Parameter;
import de.alexanderlindhorst.tomcat.session.access.FakeRiakService;

import static de.alexanderlindhorst.tomcat.session.TestUtils.getFieldValueFromObject;
import static de.alexanderlindhorst.tomcat.session.TestUtils.invokeMethod;
import static de.alexanderlindhorst.tomcat.session.TestUtils.setFieldValueForObject;
import static de.alexanderlindhorst.tomcat.session.manager.PersistableSession.SESSION_ATTRIBUTE_SET;
import static org.apache.catalina.Session.SESSION_CREATED_EVENT;
import static org.apache.catalina.Session.SESSION_DESTROYED_EVENT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RiakSessionManagerTest {

    @Mock
    private BackendService backendService;
    @Mock
    private Context context;
    @Mock
    private HttpSessionIdListener sessionIdListener;
    @Mock
    private SessionIdGenerator sessionIdGenerator;
    @Mock
    private Engine engine;
    @Mock
    private SessionListener sessionListener;
    @Captor
    private ArgumentCaptor<SessionEvent> sessionEventCaptor;
    @Captor
    private ArgumentCaptor<HttpSessionEvent> httpSessionEventCaptor;
    @InjectMocks
    private RiakSessionManager instance;

    @Before
    public void setup() throws ClassNotFoundException, IOException {
        when(sessionIdGenerator.generateSessionId()).thenReturn(UUID.randomUUID().toString());
        when(context.getParent()).thenReturn(engine);
        when(engine.getJvmRoute()).thenReturn("host");
        when(context.getName()).thenReturn("/mycontext");
        instance.load();
    }

    @After
    public void tearDown() throws IOException {
        instance.unload();
    }

    @Test
    public void propertiesCanBeReadBackAsSet() {
        instance.setServiceImplementationClassName("blub");
        assertThat(instance.getServiceImplementationClassName(), is("blub"));

        instance.setServiceSessionExpiryThreshold(17000);
        assertThat(instance.getServiceSessionExpiryThreshold(), is(17000l));
    }

    @Test
    public void getNewSessionCreatesManagerAwareRiakSession() {
        StandardSession newSession = instance.getNewSession();
        assertThat(newSession.getManager(), is((Manager) instance));
    }

    @Test
    public void createSessionCreatesSessionWithId() {
        PersistableSession emptySession = (PersistableSession) instance.createSession(null);
        assertThat(emptySession.getId(), is(notNullValue()));
    }

    @Test
    public void createSessionWithGivenIdCreatesSessionWithIdHonored() {
        String sessionId = "mySessionId";
        PersistableSession emptySession = (PersistableSession) instance.createSession(sessionId);
        assertThat(emptySession.getId(), is(sessionId));
    }

    @Test
    public void createSessionPersistsRiakSession() {
        PersistableSession newSession = (PersistableSession) instance.createSession(null);
        verify(backendService).persistSession(newSession);
    }

    @Test
    public void createSessionSignalsNewSession() throws IOException {
        String sessionId = "mySession.host1"; //context gives "host" as jvmroute
        when(backendService.getSession(any(PersistableSession.class), any(String.class))).thenReturn(null);
        when(context.getApplicationEventListeners()).thenReturn(new Object[]{sessionIdListener, sessionListener});
        instance.findSession(sessionId);

        verify(sessionListener).sessionEvent(sessionEventCaptor.capture());
        SessionEvent event = sessionEventCaptor.getValue();
        PersistableSession session = (PersistableSession) event.getSession();
        assertThat(event.getType(), is(SESSION_CREATED_EVENT));
        assertThat(session.getId(), is("mySession.host"));
        assertThat(session.getPersistenceKey(), is("mySession"));
        assertThat(session.getId(), is("mySession.host"));
    }

    @Test
    public void createSessionAttemptsToSignalNewSessionWithoutListeners() throws IOException {

        String sessionId = "mySession.host1"; //context gives "host" as jvmroute
        when(backendService.getSession(any(PersistableSession.class), any(String.class))).thenReturn(null);
        when(context.getApplicationEventListeners()).thenReturn(null);
        instance.findSession(sessionId);

        verify(sessionListener, never()).sessionEvent(any(SessionEvent.class));
        verify(sessionIdListener, never()).sessionIdChanged(any(HttpSessionEvent.class), any(String.class));
    }

    @Test
    public void findSessionRetrievesFromServiceIfNoContextRouteGiven() throws IOException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException {
        Engine engineLocal = mock(Engine.class);
        when(engineLocal.getJvmRoute()).thenReturn(null);
        Context contextLocal = mock(Context.class);
        when(contextLocal.getParent()).thenReturn(engineLocal);
        when(contextLocal.getName()).thenReturn("/mycontext");
        instance = new RiakSessionManager();
        instance.setContext(contextLocal);
        setFieldValueForObject(instance, "backendService", backendService);
        String sessionId = "mysession";

        instance.findSession(sessionId + ".host");
        verify(backendService).getSession(any(PersistableSession.class), eq(sessionId));
    }

    @Test
    public void findSessionRetrievesSessionFromServiceIfNoJVMRouteGiven() throws IOException {
        String sessionId = "mySession";
        Session found = instance.findSession(sessionId);
        verify(backendService).getSession(any(PersistableSession.class), eq(sessionId));
    }

    @Test
    public void findSessionDoesNotRetrieveSessionFromServiceIfSameJVMRoute() throws IOException {
        String sessionId = "mySession.host";
        instance.findSession(sessionId);
        verify(backendService, never()).getSession(any(PersistableSession.class), eq(sessionId));
    }

    @Test
    public void findSessionRetrievesSessionForDifferingJVMRoute() throws IOException {
        String sessionId = "mySession.host2";
        instance.findSession(sessionId);
        //lookup of id needs to be w/o JVM route
        verify(backendService).getSession(any(PersistableSession.class), eq("mySession"));
    }

    @Test
    public void findSessionRetrievesSessionFromPersistenceWithoutJVMRoute() throws IOException {
        String sessionId = "mySession";
        instance.findSession(sessionId);
        verify(backendService).getSession(any(PersistableSession.class), eq("mySession"));
    }

    @Test
    public void findSessionWontSignalNewSessionWithouthJVMRoute() throws IOException {
        //reset global variables
        engine = mock(Engine.class);
        context = mock(Context.class);
        sessionIdGenerator = mock(SessionIdGenerator.class);
        when(sessionIdGenerator.generateSessionId()).thenReturn(UUID.randomUUID().toString());
        when(context.getParent()).thenReturn(engine);
        when(context.getName()).thenReturn("/mycontext");
        when(engine.getJvmRoute()).thenReturn(null);
        instance.setContext(context);
        instance.setSessionIdGenerator(sessionIdGenerator);
        String sessionId = "mySession"; //context gives "host" as jvmroute
        PersistableSession session = new PersistableSession(instance);
        session.setId(sessionId);
        session.addSessionListener(sessionListener);
        when(backendService.getSession(any(PersistableSession.class), eq(sessionId))).thenReturn(session);

        instance.findSession(sessionId);

        verify(sessionListener, never()).sessionEvent((SessionEvent) any());
        assertThat(session.getPersistenceKey(), is("mySession"));
        assertThat(session.getId(), is("mySession"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void findSessionFailsWithNullSessionId() throws IOException {
        instance.findSession(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void findSessionFailsWithEmptySessionId() throws IOException {
        instance.findSession("");
    }

    @Test
    public void sessionCreationEventLeadsToPersisting() {
        PersistableSession session = new PersistableSession(instance);
        SessionEvent event = new SessionEvent(session, SESSION_CREATED_EVENT, session);
        instance.sessionEvent(event);
        verify(backendService).persistSession(session);
    }

    @Test
    public void sessionAttributeChangeEventLeadsToPersisting() {
        PersistableSession session = new PersistableSession(instance);
        SessionEvent event = new SessionEvent(session, SESSION_ATTRIBUTE_SET,
                new PersistableSessionAttribute("key", "value"));
        instance.sessionEvent(event);
        verify(backendService).persistSession(session);
    }

    @Test
    public void sessionExpirationEventLeadsToRemovalFromPersistence() {
        PersistableSession session = new PersistableSession(instance);
        session.setId("mysession");
        SessionEvent event = new SessionEvent(session, SESSION_DESTROYED_EVENT, null);
        instance.sessionEvent(event);
        verify(backendService).deleteSession(session);
        verify(backendService, never()).persistSession(session);
    }

    @Test(expected = AssertionError.class)
    public void unknownSessionEventTypeTriggersError() {
        instance.sessionEvent(new SessionEvent(new PersistableSession(instance), "no_such_event", null));
    }

    @Test
    public void storeCallToCleanSessionWillNotPersistSession() {
        PersistableSession session = new PersistableSession(instance);
        instance.storeSession(session);
        verify(backendService, never()).persistSession(session);
    }

    @Test
    public void storeCallToNullSessionWillSilentlyReturn() {
        PersistableSession session = null;
        instance.storeSession(session);
        verify(backendService, never()).persistSession(any(PersistableSession.class));
    }

    @Test
    public void storeCallToDirtySessionWillPersistSession() {
        PersistableSession session = new PersistableSession(instance);
        session.setDirty(true);
        instance.storeSession(session);
        verify(backendService).persistSession(session);
    }

    @Test(expected = LifecycleException.class)
    public void missingServiceImplementationMakesInitFail() throws LifecycleException {
        instance.init();
    }

    @Test(expected = LifecycleException.class)
    public void unknownServiceImplementationMakesInitFail() throws LifecycleException {
        instance.setServiceImplementationClassName("no.such.class");
        instance.init();
    }

    @Test
    public void knownServiceImplementationMakesInitSucceed() throws LifecycleException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        instance.setServiceImplementationClassName(FakeRiakService.class.getName());
        instance.init();
        assertThat(getFieldValueFromObject(instance, "backendService").getClass().getName(), is(
                FakeRiakService.class.getName()));
    }

    @Test
    public void externallySettableServiceClassNameIsReadableInternally() {
        String value = "some.class";
        instance.setServiceImplementationClassName(value);
        assertThat(instance.getServiceImplementationClassName(), is(value));
    }

    @Test
    public void externallySettableServiceBackendAddressIsReadableInternally() {
        String value = "address";
        instance.setServiceBackendAddress(value);
        assertThat(instance.getServiceBackendAddress(), is(value));
    }

    @Test
    public void startInternalWillSetStateFromStartingPrepToStarting() throws NoSuchMethodException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, LifecycleException {
        invokeMethod(instance, "setStateInternal", new Parameter(LifecycleState.class, LifecycleState.STARTING_PREP),
                new Parameter(Object.class, null), new Parameter(Boolean.TYPE, Boolean.FALSE));

        instance.startInternal();

        assertThat(instance.getState(), is(LifecycleState.STARTING));
    }

    @Test
    public void startInternalWillNotChangeStateIfNotInStartingPrepState() throws NoSuchMethodException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, LifecycleException {
        invokeMethod(instance, "setStateInternal", new Parameter(LifecycleState.class, LifecycleState.INITIALIZED),
                new Parameter(Object.class, null), new Parameter(Boolean.TYPE, Boolean.FALSE));

        instance.startInternal();

        assertThat(instance.getState(), is(LifecycleState.INITIALIZED));
    }

    @Test
    public void stopInternalWillSetStateFromStoppingPrepToStopping() throws NoSuchMethodException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, LifecycleException {
        invokeMethod(instance, "setStateInternal", new Parameter(LifecycleState.class, LifecycleState.STOPPING_PREP),
                new Parameter(Object.class, null), new Parameter(Boolean.TYPE, Boolean.FALSE));

        instance.stopInternal();

        assertThat(instance.getState(), is(LifecycleState.STOPPING));
    }

    @Test
    public void stopInternalWillNotChangeStateIfNotInStoppingPrepState() throws NoSuchMethodException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, LifecycleException {
        invokeMethod(instance, "setStateInternal", new Parameter(LifecycleState.class, LifecycleState.STOPPED),
                new Parameter(Object.class, null), new Parameter(Boolean.TYPE, Boolean.FALSE));

        instance.stopInternal();

        assertThat(instance.getState(), is(LifecycleState.STOPPED));
    }

    @Test
    public void destroyInternalHandlesNullBackendService() throws LifecycleException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException {
        setFieldValueForObject(instance, "backendService", null);
        instance.destroyInternal();
    }

    @Test
    public void destroyInternalTriggersShutDownCallOnService() throws LifecycleException {
        instance.destroyInternal();
        verify(backendService).shutdown();
    }

    @Test
    public void processExpiresCallsBackendServiceRemovalMethod() {
        instance.processExpires();
        verify(backendService).removeExpiredSessions();
    }

    @Test(expected = IllegalStateException.class)
    public void processExpiresThrowsExceptionWithoutBackendService() throws IllegalArgumentException, IllegalAccessException,
            NoSuchFieldException {
        setFieldValueForObject(instance, "backendService", null);
        instance.processExpires();
    }
}
