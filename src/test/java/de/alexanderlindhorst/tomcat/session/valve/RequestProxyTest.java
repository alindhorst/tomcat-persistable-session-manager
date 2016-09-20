/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import javax.servlet.http.Cookie;

import org.apache.catalina.connector.Request;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 *
 * @author lindhrst
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore("Implementation totally changed")
public class RequestProxyTest {

    private static final String SESSION_ID = "mySessionId";
    private static final String SESSION_ID_COOKIENAME = "JSESSIONID";
    @Mock
    private Request request;

    @Test
    public void cookiesMustIndicateProperSessionIdIfFromCookies() throws NoSuchMethodException, Throwable {
        when(request.isRequestedSessionIdFromCookie()).thenReturn(TRUE);
        when(request.isRequestedSessionIdFromURL()).thenReturn(FALSE);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(SESSION_ID_COOKIENAME, "somethingElse"),
            new Cookie("andSomething", "else")});
        RequestProxy instance = new RequestProxy(request, SESSION_ID);

        Cookie[] cookies = (Cookie[]) instance.invoke(null, Request.class.getMethod("getCookies"), null);

        assertThat(cookies, is(not(nullValue())));
        assertThat(cookies.length, is(2));
        assertThat(cookies[0].getName(), is(SESSION_ID_COOKIENAME));
        assertThat(cookies[0].getValue(), is(SESSION_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullCookiesThrowsExceptionIfSessionIdFromCookiesIndicated() throws NoSuchMethodException, Throwable {
        when(request.isRequestedSessionIdFromCookie()).thenReturn(TRUE);
        when(request.isRequestedSessionIdFromURL()).thenReturn(FALSE);
        when(request.getCookies()).thenReturn(null);
        RequestProxy instance = new RequestProxy(request, SESSION_ID);

        instance.invoke(null, Request.class.getMethod("getCookies"), null);
    }

    @Test
    public void urlMustIndicateProperSessionIdIfFromURL() throws NoSuchMethodException, Throwable {
        when(request.isRequestedSessionIdFromCookie()).thenReturn(FALSE);
        when(request.isRequestedSessionIdFromURL()).thenReturn(TRUE);

        when(request.getRequestURL()).thenReturn(new StringBuffer("http://myhost:80/mypath;jsessionid=somethingElse"));
        RequestProxy instance = new RequestProxy(request, SESSION_ID);
        String requestUrl = (instance.invoke(null, Request.class.getMethod("getRequestURL"), null)).toString();
        assertThat(requestUrl, is("http://myhost:80/mypath;jsessionid=" + SESSION_ID));

        when(request.getRequestURL()).thenReturn(new StringBuffer("http://myhost/mypath;jsessionid=somethingElse"));
        instance = new RequestProxy(request, SESSION_ID);
        requestUrl = (instance.invoke(null, Request.class.getMethod("getRequestURL"), null)).toString();
        assertThat(requestUrl, is("http://myhost/mypath;jsessionid=" + SESSION_ID));

        when(request.getRequestURL()).thenReturn(new StringBuffer("http://myhost;jsessionid=somethingElse"));
        instance = new RequestProxy(request, SESSION_ID);
        requestUrl = (instance.invoke(null, Request.class.getMethod("getRequestURL"), null)).toString();
        assertThat(requestUrl, is("http://myhost;jsessionid=" + SESSION_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void noSessionIdInUrlThrowsExceptionIfIndicatedFromUrl() throws NoSuchMethodException, Throwable {
        when(request.isRequestedSessionIdFromCookie()).thenReturn(FALSE);
        when(request.isRequestedSessionIdFromURL()).thenReturn(TRUE);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://myhost:80/mypath"));
        RequestProxy instance = new RequestProxy(request, SESSION_ID);

        String requestUrl = (instance.invoke(null, Request.class.getMethod("getRequestURL"), null)).toString();
    }

    @Test
    public void getSessionIdReturnsConstructorArg() throws NoSuchMethodException, Throwable {
        when(request.isRequestedSessionIdFromCookie()).thenReturn(TRUE);
        when(request.isRequestedSessionIdFromURL()).thenReturn(FALSE);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(SESSION_ID_COOKIENAME, "somethingElse"),
            new Cookie("andSomething", "else")});

        RequestProxy instance = new RequestProxy(request, SESSION_ID);
        String sessionId = (String) instance.invoke(null, Request.class.getMethod("getSessionInternal"), null);

        assertThat(sessionId, is(SESSION_ID));
    }

    @Test
    public void normalMethodsFallThroughToOriginalRequest() throws Throwable {
        when(request.isRequestedSessionIdFromCookie()).thenReturn(TRUE);
        when(request.isRequestedSessionIdFromURL()).thenReturn(FALSE);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(SESSION_ID_COOKIENAME, "somethingElse"),
            new Cookie("andSomething", "else")});
        String encodingExpected = "this is not a valid encoding";
        when(request.getCharacterEncoding()).thenReturn(encodingExpected);

        RequestProxy instance = new RequestProxy(request, SESSION_ID);
        String encoding = (String) instance.invoke(null, Request.class.getMethod("getCharacterEncoding"), null);

        assertThat(encoding, is(encodingExpected));
    }
}
