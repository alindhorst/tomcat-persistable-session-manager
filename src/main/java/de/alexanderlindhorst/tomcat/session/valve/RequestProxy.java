/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.regex.Matcher;

import javax.servlet.http.Cookie;

import org.apache.catalina.connector.Request;

import com.google.common.collect.Lists;

import net.sf.cglib.proxy.InvocationHandler;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.alexanderlindhorst.tomcat.session.valve.RequestUtils.URI_PATTERN;

/**
 * @author lindhrst (original author)
 */
class RequestProxy implements InvocationHandler {

    private final String sessionId;
    private final StringBuffer shadowedRequestUrl;
    private final Cookie[] shadowedCookies;
    private final Request request;

    RequestProxy(Request request, String sessionId) {
        this.request = request;
        this.sessionId = sessionId;
        this.shadowedCookies = request.isRequestedSessionIdFromCookie() ? adjustSessionIdInCookies(request.getCookies(),
                sessionId) :
                 request.getCookies();
        this.shadowedRequestUrl = request.isRequestedSessionIdFromURL() ? adjustUrlString(
                request.getRequestURL().toString(), sessionId) :
                 request.getRequestURL();
    }

    private Cookie[] getCookies() {
        return shadowedCookies;
    }

    private StringBuffer getRequestURL() {
        return new StringBuffer(shadowedRequestUrl);
    }

    static Cookie[] adjustSessionIdInCookies(Cookie[] originalCookies, String sessionId) {
        if (originalCookies == null) {
            throw new IllegalArgumentException("Method must only be called if there is a session Id in the cookies");
        }
        ArrayList<Cookie> cookieList = Lists.newArrayList(originalCookies);
        cookieList.stream().forEach(cookie -> {
            if (cookie.getName().equals("JSESSIONID")) {
                cookie.setValue(sessionId);
            }
        });
        return cookieList.toArray(new Cookie[originalCookies.length]);
    }

    private static StringBuffer adjustUrlString(String requestUrl, String sessionId) {
        Matcher matcher = URI_PATTERN.matcher(requestUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Method must only be called if jsessionid is contained in URL");
        }
        StringBuffer builder = new StringBuffer();
        builder.append(matcher.group("protocol")).append("://").append(matcher.group("host"));

        if (!isNullOrEmpty(matcher.group("port"))) {
            builder.append(matcher.group("port"));
        }

        if (!isNullOrEmpty(matcher.group("path"))) {
            builder.append(matcher.group("path"));
        }
        //always !=null if matcher matches
        builder.append(";jsessionid=").append(sessionId);

        return builder;
    }

    private String getSessionInternal() {
        return sessionId;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("getCookies")) {
            return getCookies();
        }
        if (method.getName().equals("getSessionInternal")) {
            return getSessionInternal();
        }
        if (method.getName().equals("getRequestURL")) {
            return getRequestURL();
        }

        return method.invoke(request, args);
    }
}
