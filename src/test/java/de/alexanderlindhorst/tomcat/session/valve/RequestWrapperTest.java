/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import javax.servlet.http.HttpServletRequest;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author lindhrst
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore
public class RequestWrapperTest {

    private static final String SESSION_ID = "updated";
    @Mock
    private HttpServletRequest request;

//    @Test
//    public void cookiesHaveUpdatedSessionIdIfIndicatedByFlags() {
//        Cookie[] cookies = new Cookie[]{
//            new Cookie("some", "cookie"),
//            new Cookie("JSESSIONID", "something")
//        };
//        when(request.isRequestedSessionIdFromCookie()).thenReturn(true);
//        when(request.isRequestedSessionIdFromURL()).thenReturn(false);
//        RequestWrapper wrapper = new RequestWrapper(SESSION_ID, cookies, request);
//
//        assertThat(wrapper.getCookies(), is(not(nullValue())));
//        assertThat(wrapper.getCookies().length, is(2));
//        assertThat(wrapper.getCookies()[1].getName(), is("JSESSIONID"));
//        assertThat(wrapper.getCookies()[1].getValue(), is(SESSION_ID));
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void illegalArgumentThrownIfCookiesIndicatedByFlagButNotThere() {
//        when(request.isRequestedSessionIdFromCookie()).thenReturn(true);
//        when(request.isRequestedSessionIdFromURL()).thenReturn(false);
//        new RequestWrapper("sessionId", null, request);
//    }
//
//    @Test
//    public void requestURLHasUpdatedSessionIdIfIndicatedByFlags() {
//        when(request.isRequestedSessionIdFromCookie()).thenReturn(false);
//        when(request.isRequestedSessionIdFromURL()).thenReturn(true);
//
//        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host:80/path;jsessionid=xyz.ab"));
//        RequestWrapper requestWrapper = new RequestWrapper(SESSION_ID, null, request);
//        assertThat(requestWrapper.getRequestURL().toString(), is("http://host:80/path;jsessionid=updated"));
//
//        when(request.getRequestURL()).thenReturn(new StringBuffer("https://host:80/path;jsessionid=xyz.ab"));
//        requestWrapper = new RequestWrapper(SESSION_ID, null, request);
//        assertThat(requestWrapper.getRequestURL().toString(), is("https://host:80/path;jsessionid=updated"));
//
//        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host/path;jsessionid=xyz.ab"));
//        requestWrapper = new RequestWrapper(SESSION_ID, null, request);
//        assertThat(requestWrapper.getRequestURL().toString(), is("http://host/path;jsessionid=updated"));
//
//        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host:80/path;jsessionid=xyz.ab"));
//        requestWrapper = new RequestWrapper(SESSION_ID, null, request);
//        assertThat(requestWrapper.getRequestURL().toString(), is("http://host:80/path;jsessionid=updated"));
//
//        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host:80;jsessionid=xyz.ab"));
//        requestWrapper = new RequestWrapper(SESSION_ID, null, request);
//        assertThat(requestWrapper.getRequestURL().toString(), is("http://host:80;jsessionid=updated"));
//    }
//
//    @Test(expected = IllegalStateException.class)
//    public void requestURLCannotBeAdjustedIfNotIndicatedByFlags() {
//        when(request.isRequestedSessionIdFromCookie()).thenReturn(false);
//        when(request.isRequestedSessionIdFromURL()).thenReturn(true);
//        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host:80/path"));
//        RequestWrapper requestWrapper = new RequestWrapper(SESSION_ID, null, request);
//    }
//
//    @Test
//    public void getSessionIdReturnsConstructorParameterValue() {
//        when(request.isRequestedSessionIdFromCookie()).thenReturn(false);
//        when(request.isRequestedSessionIdFromURL()).thenReturn(false);
//        RequestWrapper requestWrapper = new RequestWrapper(SESSION_ID, null, request);
//        assertThat(requestWrapper.getSessionId(), is(SESSION_ID));
//    }
}
