/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import java.util.ArrayList;
import java.util.regex.Matcher;

import javax.servlet.http.Cookie;

import org.apache.catalina.connector.Request;

import com.google.common.collect.Lists;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.alexanderlindhorst.tomcat.session.valve.RequestUtils.URI_PATTERN;

/**
 * @author lindhrst (original author)
 */
class RequestWrapper extends Request {

    private final String sessionId;
    private final StringBuffer shadowedRequestUrl;
    private final Cookie[] shadowedCookies;

    public RequestWrapper(org.apache.coyote.Request nativeRequest, String sessionId) {
        setCoyoteRequest(coyoteRequest);
        this.sessionId = sessionId;
        this.shadowedCookies = super.isRequestedSessionIdFromCookie() ? adjustSessionIdInCookies(super.getCookies(), sessionId)
                               : super.getCookies();
        this.shadowedRequestUrl = super.isRequestedSessionIdFromURL() ? adjustUrlString(super.getRequestURL().toString(), sessionId)
                                  : super.getRequestURL();
    }

    @Override
    public Cookie[] getCookies() {
        return shadowedCookies;
    }

    @Override
    public StringBuffer getRequestURL() {
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
