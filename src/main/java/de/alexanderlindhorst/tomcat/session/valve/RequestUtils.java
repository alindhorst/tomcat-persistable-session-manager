/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.catalina.connector.Request;

import javax.servlet.http.Cookie;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author lindhrst (original author)
 */
final class RequestUtils {

    static final Pattern URI_PATTERN = Pattern.compile(
            "((?<protocol>https?)://(?<host>[^?/:;]+)(?<port>:\\d+)?)?(?<path>/[^;\\?]*)?(?<sessionpart>;jsessionid=(?<sessionid>[^\\?]+))");
    static final Pattern SESSION_ID_PATTERN = Pattern.compile("^(?<sessionId>[^\\.]+)(\\.(?<jvmRoute>.*))?$");

    private RequestUtils() {
        //utility class not to be instantiated
    }

    static String getSessionIdFromRequest(Request request) {
        if (request.isRequestedSessionIdFromURL()) {
            Matcher matcher = URI_PATTERN.matcher(request.getRequestURL().toString());
            if (!matcher.matches()) {
                throw new IllegalStateException("Method must only be called when session is is URL");
            }
            return matcher.group("sessionid");
        }
        if (request.isRequestedSessionIdFromCookie()) {
            Cookie[] cookies = request.getCookies();
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("JSESSIONID")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    static String getSessionJvmRouteFromRequest(Request request) {
        String sessionIdFromRequest = getSessionIdFromRequest(request);
        if (!isNullOrEmpty(sessionIdFromRequest)) {
            Matcher matcher = SESSION_ID_PATTERN.matcher(sessionIdFromRequest);
            if (matcher.matches()) {
                return matcher.group("sessionId");
            }
        }
        return null;
    }
}
