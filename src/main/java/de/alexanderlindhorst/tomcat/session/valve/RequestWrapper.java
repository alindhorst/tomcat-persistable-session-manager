/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import java.util.ArrayList;
import java.util.regex.Matcher;

import com.google.common.collect.Lists;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.alexanderlindhorst.tomcat.session.valve.RequestUtils.URI_PATTERN;

/**
 * @author lindhrst (original author)
 */
class RequestWrapper extends HttpServletRequestWrapper {

    private final String sessionId;
    private final StringBuffer requestURL;
    private final Cookie[] cookies;

    public RequestWrapper(String sessionId, Cookie[] cookies, HttpServletRequest request) {
        super(request);
        this.sessionId = sessionId;
        this.cookies = request.isRequestedSessionIdFromCookie() ? adjustSessionIdInCookies(cookies, sessionId) : cookies;
        this.requestURL = request.isRequestedSessionIdFromURL() ? adjustUrlString(request, sessionId) :
                request.getRequestURL();
    }

    @Override
    public Cookie[] getCookies() {
        return cookies;
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(requestURL);
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

    private static StringBuffer adjustUrlString(HttpServletRequest request, String sessionId) {
        Matcher matcher = URI_PATTERN.matcher(request.getRequestURL().toString());
        if (!matcher.matches()) {
            throw new IllegalStateException("Method must only be called if jsessionid is contained in URL");
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

    public String getSessionId() {
        return sessionId;
    }
}
