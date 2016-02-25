/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;

import org.apache.catalina.connector.Request;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author lindhrst (original author)
 */
final class RequestUtils {

    private static final String SESSION_ID = "sessionId";
    private static final String JVM_ROUTE = "jvmRoute";
    static final Pattern URI_PATTERN = Pattern.compile(
            "((?<protocol>https?)://(?<host>[^?/:;]+)(?<port>:\\d+)?)?(?<path>/[^;?]*)?(?<sessionpart>;jsessionid=(?<" + SESSION_ID
            + ">[^?]+))");
    static final Pattern SESSION_ID_PATTERN = Pattern.compile("^(?<" + SESSION_ID + ">[^\\.]+)(\\.(?<" + JVM_ROUTE + ">.*))?$");

    private RequestUtils() {
        //utility class not to be instantiated
    }

    /**
     * @param request
     * @return session id
     * @throws NullPointerException if no session id can be found when the underlying request signals that there should be one
     */
    static String getSessionIdFromRequest(Request request) {
        if (!(request.isRequestedSessionIdFromCookie() || request.isRequestedSessionIdFromURL())) {
            return null;
        }
        if (request.isRequestedSessionIdFromURL()) {
            Matcher matcher = URI_PATTERN.matcher(request.getRequestURL().toString());
            if (!matcher.matches()) {
                throw new IllegalStateException("No session id found in request even though indicated by flags");
            }
            return matcher.group(SESSION_ID);
        } else { // must be in cookie
            Cookie[] cookies = request.getCookies();
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("JSESSIONID") && !isNullOrEmpty(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
            throw new IllegalStateException("No session cookie found even though indicated by flags");
        }
    }

    static String getSessionJvmRouteFromRequest(Request request) {
        String sessionIdFromRequest = getSessionIdFromRequest(request);
        if (!isNullOrEmpty(sessionIdFromRequest)) {
            Matcher matcher = SESSION_ID_PATTERN.matcher(sessionIdFromRequest);
            matcher.lookingAt();
            return matcher.group(JVM_ROUTE);
        }
        return null;
    }

    static String getSessionIdInternalFromRequest(Request request) {
        String sessionIdFromRequest = getSessionIdFromRequest(request);
        if (!isNullOrEmpty(sessionIdFromRequest)) {
            Matcher matcher = SESSION_ID_PATTERN.matcher(sessionIdFromRequest);
            matcher.lookingAt();
            return matcher.group(SESSION_ID);
        } else {
            return null;
        }
    }
}
