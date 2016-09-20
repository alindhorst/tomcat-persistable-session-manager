/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import java.lang.reflect.Method;
import java.util.regex.Matcher;

import org.apache.catalina.connector.Request;

import net.sf.cglib.proxy.InvocationHandler;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.alexanderlindhorst.tomcat.session.valve.RequestUtils.URI_PATTERN;

/**
 * Proxy request implementation to force {@link Request#getRequestURL() } to reflect an updated session ID string even
 * if the session ID comes from a cookie. If the session ID is from a cookie Tomcat will normally not adjust session ID
 * Strings in URLs, this method changes that behavior.
 *
 * @author lindhrst (original author)
 */
class RequestProxy implements InvocationHandler {

    private final String sessionId;
    private final Request request;

    RequestProxy(Request request, String sessionId) {
        this.request = request;
        this.sessionId = sessionId;
    }

    private String encodeURL(String url, String sessionId) {
        return adjustUrlString(url, sessionId);
    }

    private static String adjustUrlString(String requestUrl, String sessionId) {
        Matcher matcher = URI_PATTERN.matcher(requestUrl);
        if (!matcher.matches()) {
            return requestUrl;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(matcher.group("protocol")).append("://").append(matcher.group("host"));

        if (!isNullOrEmpty(matcher.group("port"))) {
            builder.append(matcher.group("port"));
        }

        if (!isNullOrEmpty(matcher.group("path"))) {
            builder.append(matcher.group("path"));
        }
        //always !=null if matcher matches
        builder.append(";jsessionid=").append(sessionId);

        return builder.toString();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("encodeURL")) {
            return encodeURL((String) args[0], sessionId);
        }

        return method.invoke(request, args);
    }
}
