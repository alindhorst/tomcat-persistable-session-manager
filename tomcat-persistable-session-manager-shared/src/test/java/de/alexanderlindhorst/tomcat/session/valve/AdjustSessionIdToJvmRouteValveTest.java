/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import de.alexanderlindhorst.tomcat.session.manager.PersistableSession;
import de.alexanderlindhorst.tomcat.session.manager.PersistableSessionManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

/**
 *
 * @author alindhorst
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AdjustSessionIdToJvmRouteValveTest {

    @Mock
    private Request request;
    @Mock
    private Response response;
    @Mock
    private Context context;
    @Mock
    private Manager genericManager;
    @Mock
    private Valve next;
    @Mock
    private PersistableSessionManager wellKnownManager;
    @Mock
    private PersistableSession session;

    private AdjustSessionIdToJvmRouteValve valve;

    @Before
    public void setup() {
        valve = new AdjustSessionIdToJvmRouteValve();
        valve.setNext(next);
        when(request.getContext()).thenReturn(context);
    }

    @Test
    public void noChangesMadeToRequestForUnknownManager() throws IOException, ServletException {
        when(context.getManager()).thenReturn(genericManager);

        valve.invoke(request, response);

        verify(request, never()).changeSessionId(any(String.class));
    }

    @Test
    public void noChangesMadeToRequestForNonExistentSession() throws IOException, ServletException {
        when(context.getManager()).thenReturn(wellKnownManager);
        when(request.getSession(false)).thenReturn(null);

        valve.invoke(request, response);

        verify(request, never()).changeSessionId(any(String.class));
    }

    @Test
    public void noChangesMadeToRequestIfSameSessionId() throws IOException, ServletException {
        String sessionId = "sessionId";
        when(context.getManager()).thenReturn(wellKnownManager);
        when(request.getSession(false)).thenReturn(session);
        when(request.getRequestedSessionId()).thenReturn(sessionId);
        when(session.getId()).thenReturn(sessionId);

        valve.invoke(request, response);

        verify(request, never()).changeSessionId(any(String.class));
    }

    @Test
    public void noChangesMadeToRequestIfNoSessionId() throws IOException, ServletException {
        String sessionId = "sessionId";
        when(context.getManager()).thenReturn(wellKnownManager);
        when(request.getSession(false)).thenReturn(session);
        when(request.getRequestedSessionId()).thenReturn(null);
        when(session.getId()).thenReturn(sessionId);

        valve.invoke(request, response);

        verify(request, never()).changeSessionId(any(String.class));
    }

    @Test
    public void requestSessionIdChangedToChangedSessionId() throws IOException, ServletException {
        String oldId = "sessionId";
        String newId = "sessionId.new";
        when(context.getManager()).thenReturn(wellKnownManager);
        when(request.getSession(false)).thenReturn(session);
        when(request.getRequestedSessionId()).thenReturn(oldId);
        when(session.getId()).thenReturn(newId);

        valve.invoke(request, response);

        verify(request).changeSessionId(newId);
    }

    @Test
    public void changeSessionIdIsCalledSoEncodeURLReflectsRewrittenJvmRouteSuffix() throws IOException, ServletException {
        // When a request arrives with a session ID from a different JVM node (e.g. "sessionId.node2")
        // and the manager rewrites it to the local JVM route (e.g. "sessionId.node1"), the valve
        // must call request.changeSessionId() so that subsequent response.encodeURL() calls embed
        // the updated ID rather than the stale one from the request.
        String incomingId = "sessionId.node2";
        String rewrittenId = "sessionId.node1";
        when(context.getManager()).thenReturn(wellKnownManager);
        when(request.getSession(false)).thenReturn(session);
        when(request.getRequestedSessionId()).thenReturn(incomingId);
        when(session.getId()).thenReturn(rewrittenId);

        valve.invoke(request, response);

        verify(request).changeSessionId(rewrittenId);
    }
}
