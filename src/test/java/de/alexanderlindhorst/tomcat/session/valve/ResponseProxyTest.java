/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import org.apache.catalina.connector.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author lindhrst
 */
@RunWith(MockitoJUnitRunner.class)
public class ResponseProxyTest {

    private static final String SESSION_ID = "mySessionId";
    private static final String SESSION_ID_COOKIENAME = "JSESSIONID";
    @Mock
    private Response response;

    @Test
    public void urlMustIndicateProperSessionIdIfFromURL() throws NoSuchMethodException, Throwable {
        ResponseProxy instance = new ResponseProxy(response, SESSION_ID);

        String location = "http://myhost:80/mypath;jsessionid=somethingElse";
        String encodedUrl = (String) instance.invoke(null, Response.class.getMethod("encodeURL", String.class),
                new Object[]{location});
        assertThat(encodedUrl, is("http://myhost:80/mypath;jsessionid=" + SESSION_ID));

        location = "http://myhost/mypath;jsessionid=somethingElse";
        encodedUrl = (String) instance.invoke(null, Response.class.getMethod("encodeURL", String.class), new Object[]{
            location});
        assertThat(encodedUrl, is("http://myhost/mypath;jsessionid=" + SESSION_ID));

        location = "http://myhost;jsessionid=somethingElse";
        encodedUrl = (String) instance.invoke(null, Response.class.getMethod("encodeURL", String.class), new Object[]{
            location});
        assertThat(encodedUrl, is("http://myhost;jsessionid=" + SESSION_ID));
    }

    @Test
    public void noSessionIdInUrlReturnsInputValue() throws NoSuchMethodException, Throwable {
        String location = "http://myhost:80/mypath";
        ResponseProxy instance = new ResponseProxy(response, SESSION_ID);

        String requestUrl = (String) instance.invoke(null, Response.class.getMethod("encodeURL", String.class),
                new Object[]{location});

        assertThat(requestUrl, is(location));
    }
}
