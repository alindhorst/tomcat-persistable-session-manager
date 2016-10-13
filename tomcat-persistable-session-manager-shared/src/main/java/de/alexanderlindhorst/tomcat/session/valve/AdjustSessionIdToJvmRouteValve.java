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

import de.alexanderlindhorst.tomcat.session.manager.PersistableSessionManager;

import static com.google.common.base.Strings.isNullOrEmpty;

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
        if (!(m instanceof PersistableSessionManager)) {
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

        String requestSessionId = request.getRequestedSessionId();
        if (!isNullOrEmpty(requestSessionId)) {
            //there is a session id in the request that we potentially need to modify
            if (!requestSessionId.equals(session.getId())) {
                //the session manager comes back with a different session id than found in the request
                request.changeSessionId(session.getId());
                request.setAttribute(ORIGINAL_ID_ATTRIBUTE, session.getId());
            }
        }

        getNext().invoke(request, response);
    }
}
