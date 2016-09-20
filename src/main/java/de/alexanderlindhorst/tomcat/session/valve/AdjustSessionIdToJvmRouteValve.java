/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Manager;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.alexanderlindhorst.riak.session.manager.RiakSessionManager;

import static com.google.common.base.Strings.isNullOrEmpty;
import static net.sf.cglib.proxy.Enhancer.create;

/**
 * @author lindhrst (original author)
 */
public class AdjustSessionIdToJvmRouteValve extends ValveBase {

    private static final String ORIGINAL_ID_ATTRIBUTE = "org.apache.catalina.ha.session.JvmRouteOrignalSessionID";
    private static final Logger LOGGER = LoggerFactory.getLogger(AdjustSessionIdToJvmRouteValve.class);

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        //not a known manager -> no optimization
        Manager m = request.getContext().getManager();
        if (!(m instanceof RiakSessionManager)) {
            LOGGER.debug("No compatible session manager found, skipping execution");
            getNext().invoke(request, response);
            return;
        }

        //if no session, just continue normally
        HttpSession session = request.getSession(false);
        if (session == null) {
            getNext().invoke(request, response);
            return;
        }

        String sessionId = request.getRequestedSessionId();
        if (isNullOrEmpty(sessionId)) {
            //no change, continue normally
            getNext().invoke(request, response);
            return;
        }

        if (!sessionId.equals(session.getId())) {
            //there was a session in the request and it differs from what the request has -> adjust it
            request.changeSessionId(session.getId());
            request.setAttribute(ORIGINAL_ID_ATTRIBUTE, session.getId());
        }

        //if sesison id comes from cookie make sure URL encoding works consistently
        if (request.isRequestedSessionIdFromCookie()) {
            //if session comes from somewhere else enforce consistent results for encodeURL
            getNext().invoke(request, (Response) create(Response.class, new ResponseProxy(response, session.getId())));
        } else {
            getNext().invoke(request, response);
        }
    }
}
