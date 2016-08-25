/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.alexanderlindhorst.riak.session.manager.PersistableSession;
import de.alexanderlindhorst.riak.session.manager.RiakSessionManager;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author alindhorst
 */
@RunWith(MockitoJUnitRunner.class)
public class AdjustSessionIdToJvmRouteValveTest {

    @Mock
    private Request request;
    private org.apache.coyote.Request coyoteRequest;
    @Mock
    private Response response;
    @Mock
    private Context context;
    @Mock
    private Manager genericManager;
    @Mock
    private Valve next;
    @Mock
    private RiakSessionManager wellKnownManager;
    @Mock
    private PersistableSession session;

    private AdjustSessionIdToJvmRouteValve valve;

    @Before
    public void setup() {
        coyoteRequest=new org.apache.coyote.Request();
        valve = new AdjustSessionIdToJvmRouteValve();
        valve.setNext(next);
        when(request.getContext()).thenReturn(context);
    }

    @Test
    public void genericManagerCausesByPass() throws IOException, ServletException {
        when(context.getManager()).thenReturn(genericManager);
        valve.invoke(request, response);
        verify(next).invoke(request, response);
    }

    @Test
    public void wellKnownManagerWithoutRouteCausesByPass() throws IOException, ServletException {
        when(context.getManager()).thenReturn(wellKnownManager);
        valve.invoke(request, response);
        verify(next).invoke(request, response);
    }

    @Test
    public void requestWithoutRouteCausesByPass() throws IOException, ServletException {
        when(context.getManager()).thenReturn(wellKnownManager);
        when(wellKnownManager.getJvmRoute()).thenReturn("route");
        valve.invoke(request, response);
        verify(next).invoke(request, response);
    }

    @Test
    public void requestWithSameRouteAsManagerCausesByPass() throws IOException, ServletException {
        when(context.getManager()).thenReturn(wellKnownManager);
        when(wellKnownManager.getJvmRoute()).thenReturn("route");
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.FALSE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.TRUE);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("JSESSIONID", "id.route")});

        valve.invoke(request, response);

        verify(next).invoke(request, response);
    }

    @Test
    @Ignore("logic changed, runtime tests pending, ignored")
    public void requestWithDifferentRouteAsManagerCausesSessionFetch() throws IOException, ServletException {
        when(context.getManager()).thenReturn(wellKnownManager);
        when(wellKnownManager.getJvmRoute()).thenReturn("route");
        when(wellKnownManager.findSession(anyString())).thenReturn(session);
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.FALSE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.TRUE);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("JSESSIONID", "id.route2")});
        when(request.getCoyoteRequest()).thenReturn(coyoteRequest);
        coyoteRequest.scheme().setString("http");

        valve.invoke(request, response);

        verify(wellKnownManager).add(session);
        verify(next,never()).invoke(request, response);
    }
}
