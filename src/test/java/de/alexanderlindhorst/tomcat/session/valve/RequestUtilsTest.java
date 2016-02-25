/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import javax.servlet.http.Cookie;

import org.apache.catalina.connector.Request;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static de.alexanderlindhorst.tomcat.session.valve.RequestUtils.getSessionIdFromRequest;
import static de.alexanderlindhorst.tomcat.session.valve.RequestUtils.getSessionIdInternalFromRequest;
import static de.alexanderlindhorst.tomcat.session.valve.RequestUtils.getSessionJvmRouteFromRequest;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 *
 * @author lindhrst
 */
//@Ignore("TODO: Implement these tests")
@RunWith(MockitoJUnitRunner.class)
public class RequestUtilsTest {

    private static final String JVM_ROUTE = "host";
    private static final String SHORT_ID = "sessionid";
    private static final String LONG_ID = SHORT_ID + "." + JVM_ROUTE;

    @Mock
    private Request request;

    @Test
    public void sessionIdCanBeReadFromUrl() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host:1234/path;jsessionid=" + SHORT_ID),
                new StringBuffer("http://host:3434/path;jsessionid=" + LONG_ID));
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.TRUE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.FALSE);
        assertThat(getSessionIdFromRequest(request), is(SHORT_ID));
        assertThat(getSessionIdFromRequest(request), is(LONG_ID));
    }

    @Test
    public void sessionIdCanBeReadFromCookie() {
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.FALSE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.TRUE);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("JSESSIONID", SHORT_ID)}, new Cookie[]{new Cookie("JSESSIONID",
            LONG_ID)});

        assertThat(getSessionIdFromRequest(request), is(SHORT_ID));
        assertThat(getSessionIdFromRequest(request), is(LONG_ID));
    }

    @Test
    public void noSessionIdReturnsNullSessionId() {
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.FALSE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.FALSE);

        assertThat(getSessionIdFromRequest(request), is(nullValue()));
    }

    @Test
    public void jvmRouteCanBeReadFromUrl() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host:3434/path;jsessionid=" + LONG_ID));
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.TRUE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.FALSE);
        assertThat(getSessionJvmRouteFromRequest(request), is(JVM_ROUTE));
    }

    @Test
    public void jvmRouteCanBeReadFromCookie() {
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.FALSE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.TRUE);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("JSESSIONID", LONG_ID)});

        assertThat(getSessionJvmRouteFromRequest(request), is(JVM_ROUTE));
    }

    @Test
    public void noSessionIdFromFlagsReturnsNullJvmRoute() {
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.FALSE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.FALSE);
        assertThat(getSessionJvmRouteFromRequest(request), is(nullValue()));
    }

    @Test
    public void sessionIdInteranlCanBeReadFromUrl() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host:1234/path;jsessionid=" + SHORT_ID),
                new StringBuffer("http://host:3434/path;jsessionid=" + LONG_ID));
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.TRUE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.FALSE);
        assertThat(getSessionIdInternalFromRequest(request), is(SHORT_ID));
        assertThat(getSessionIdInternalFromRequest(request), is(SHORT_ID));
    }

    @Test
    public void sessionIdInternalCanBeReadFromCookie() {
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.FALSE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.TRUE);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("JSESSIONID", SHORT_ID)}, new Cookie[]{new Cookie("JSESSIONID",
            LONG_ID)});

        assertThat(getSessionIdInternalFromRequest(request), is(SHORT_ID));
        assertThat(getSessionIdInternalFromRequest(request), is(SHORT_ID));
    }

    @Test
    public void emptySessionIdReturnsNullForSessionInternal() {
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.FALSE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.FALSE);
        
        assertThat(getSessionIdInternalFromRequest(request), is(nullValue()));
    }

    @Test(expected = IllegalStateException.class)
    public void noCookiesEvenThoughFlaggedThrowsIllegalStateExeption() {
        when(request.getCookies()).thenReturn(new Cookie[]{});
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.TRUE);
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.FALSE);
        getSessionIdFromRequest(request);
    }

    @Test(expected = IllegalStateException.class)
    public void noSessionIDFromCookiesEvenThoughFlaggedThrowsIllegalStateExeption() {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("something", "else")});
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.TRUE);
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.FALSE);
        getSessionIdFromRequest(request);
    }

    @Test(expected = IllegalStateException.class)
    public void emptySessionIDFromCookiesEvenThoughFlaggedThrowsIllegalStateExeption() {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("JSESSIONID", "")});
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.TRUE);
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.FALSE);
        getSessionIdFromRequest(request);
    }

    @Test(expected = IllegalStateException.class)
    public void noSessionIDInUrlButIndicatedByFlagsThrowsIllegalStateException() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host:1234/path"));
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.TRUE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.FALSE);
        getSessionIdFromRequest(request);
    }

    @Test
    public void noJvmRouteInURLSessionIdReturnsNullValue() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host:1234/path;jsessionid=" + SHORT_ID));
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.TRUE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.FALSE);
        assertThat(getSessionJvmRouteFromRequest(request), is(nullValue()));
    }

    @Test(expected = IllegalStateException.class)
    public void emptySessionIDInUrlButIndicatedByFlagsThrowsIllegalStateException() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host:1234/path;jessionid="));
        when(request.isRequestedSessionIdFromURL()).thenReturn(Boolean.TRUE);
        when(request.isRequestedSessionIdFromCookie()).thenReturn(Boolean.FALSE);
        getSessionIdFromRequest(request);
    }
}
