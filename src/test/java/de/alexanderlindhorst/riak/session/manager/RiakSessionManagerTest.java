/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.manager;

import java.io.IOException;
import java.util.UUID;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionIdGenerator;
import org.apache.catalina.SessionListener;
import org.apache.catalina.session.StandardSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.alexanderlindhorst.riak.session.access.FakeRiakService;
import de.alexanderlindhorst.riak.session.access.RiakService;

import static de.alexanderlindhorst.riak.session.TestUtils.getFieldValueFromObject;
import static de.alexanderlindhorst.riak.session.manager.RiakSession.SESSION_ATTRIBUTE_SET;
import static org.apache.catalina.Session.SESSION_CREATED_EVENT;
import static org.apache.catalina.Session.SESSION_DESTROYED_EVENT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RiakSessionManagerTest {

    @Mock
    private RiakService riakService;
    @Mock
    private Context context;
    @Mock
    private SessionIdGenerator sessionIdGenerator;
    @Mock
    private Engine engine;
    @Mock
    private SessionListener sessionListener;
    @Captor
    private ArgumentCaptor<SessionEvent> sessionEventCaptor;
    @InjectMocks
    private RiakSessionManager instance;

    @Before
    public void setup() {
        when(sessionIdGenerator.generateSessionId()).thenReturn(UUID.randomUUID().toString());
        when(context.getParent()).thenReturn(engine);
        when(engine.getJvmRoute()).thenReturn("host");
        when(context.getName()).thenReturn("/mycontext");
    }

    @Test
    public void getNewSessionCreatesManagerAwareRiakSession() {
        StandardSession newSession = instance.getNewSession();
        assertThat(newSession.getManager(), is((Manager) instance));
    }

    @Test
    public void createSessionCreatesSessionWithId() {
        RiakSession emptySession = (RiakSession) instance.createSession(null);
        assertThat(emptySession.getId(), is(notNullValue()));
    }

    @Test
    public void createSessionWithGivenIdCreatesSessionWithIdHonored() {
        String sessionId = "mySessionId";
        RiakSession emptySession = (RiakSession) instance.createSession(sessionId);
        assertThat(emptySession.getId(), is(sessionId));
    }

    @Test
    public void createSessionPersistsRiakSession() {
        RiakSession newSession = (RiakSession) instance.createSession(null);
        verify(riakService).persistSession(newSession);
    }

    @Test
    public void findSessionRetrievesSessionFromServiceIfNoJVMRouteGiven() throws IOException {
        String sessionId = "mySession";
        Session found = instance.findSession(sessionId);
        verify(riakService).getSession(sessionId);
    }

    @Test
    public void findSessionDoesNotRetrieveSessionFromServiceIfSameJVMRoute() throws IOException {
        String sessionId = "mySession.host";
        instance.findSession(sessionId);
        verify(riakService, never()).getSession(sessionId);
    }

    @Test
    public void findSessionRetrievesSessionForDifferingJVMRoute() throws IOException {
        String sessionId = "mySession.host2";
        instance.findSession(sessionId);
        //lookup of id needs to be w/o JVM route
        verify(riakService).getSession("mySession");
    }

    @Test
    public void findSessionRetrievesSessionFromPersistenceWithoutJVMRoute() throws IOException {
        String sessionId = "mySession";
        instance.findSession(sessionId);
        verify(riakService).getSession("mySession");
    }

    @Test
    public void findSessionSignalsNewSessionWhenGoingToPersistenceAndDifferentJVMRoute() throws IOException {
        String sessionId = "mySession.host1"; //context gives "host" as jvmroute
        RiakSession session = new RiakSession(instance);
        session.setId(sessionId);
        session.addSessionListener(sessionListener);
        when(riakService.getSession(sessionId)).thenReturn(session);
        when(riakService.getSession("mySession")).thenReturn(session);
        instance.findSession(sessionId);

        verify(sessionListener).sessionEvent(sessionEventCaptor.capture());
        assertThat(session.getIdInternal(), is("mySession"));
        assertThat(session.getId(), is("mySession.host"));
        assertThat(sessionEventCaptor.getValue().getType(), is(Session.SESSION_CREATED_EVENT));
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
        RiakSession session = new RiakSession(instance);
        session.setId(sessionId);
        session.addSessionListener(sessionListener);
        when(riakService.getSession(sessionId)).thenReturn(session);

        instance.findSession(sessionId);

        verify(sessionListener, never()).sessionEvent((SessionEvent) any());
        assertThat(session.getIdInternal(), is("mySession"));
        assertThat(session.getId(), is("mySession"));
    }

    @Test
    public void sessionCreationEventLeadsToPersisting() {
        RiakSession session = new RiakSession(instance);
        SessionEvent event = new SessionEvent(session, SESSION_CREATED_EVENT, session);
        instance.sessionEvent(event);
        verify(riakService).persistSession(session);
    }

    @Test
    public void sessionAttributeChangeEventLeadsToPersisting() {
        RiakSession session = new RiakSession(instance);
        SessionEvent event = new SessionEvent(session, SESSION_ATTRIBUTE_SET,
                new PersistableSessionAttribute("key", "value"));
        instance.sessionEvent(event);
        verify(riakService).persistSession(session);
    }

    @Test
    public void sessionExpirationEventLeadsToRemovalFromPersistence() {
        RiakSession session = new RiakSession(instance);
        session.setId("mysession");
        SessionEvent event = new SessionEvent(session, SESSION_DESTROYED_EVENT, null);
        instance.sessionEvent(event);
        verify(riakService).deleteSession(session);
        verify(riakService, never()).persistSession(session);
    }

    @Test(expected = AssertionError.class)
    public void unknownSessionEventTypeTriggersError() {
        instance.sessionEvent(new SessionEvent(new RiakSession(instance), "no_such_event", null));
    }

    @Test
    public void storeCallToCleanSessionWillNotPersistSession() {
        RiakSession session = new RiakSession(instance);
        instance.storeSession(session);
        verify(riakService, never()).persistSession(session);
    }

    @Test(expected = LifecycleException.class)
    public void missingRiakServiceImplementationMakesInitFail() throws LifecycleException {
        instance.init();
    }

    @Test(expected = LifecycleException.class)
    public void unknownRiakServiceImplementationMakesInitFail() throws LifecycleException {
        instance.setRiakServiceImplementationClassName("no.such.class");
        instance.init();
    }

    @Test
    public void knownRiakServiceImplementationMakesInitSucceed() throws LifecycleException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        instance.setRiakServiceImplementationClassName(FakeRiakService.class.getName());
        instance.init();
        assertThat(getFieldValueFromObject(instance, "riakService").getClass().getName(), is(
                FakeRiakService.class.getName()));
    }

    @Test
    public void externallySettableRiakServiceClassNameIsReadableInternally() {
        instance.setRiakServiceImplementationClassName("some.class");
        assertThat(instance.getRiakServiceImplementationClassName(), is("some.class"));
    }
}
