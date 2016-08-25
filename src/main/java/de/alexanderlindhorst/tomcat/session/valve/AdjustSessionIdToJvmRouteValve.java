/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Manager;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import de.alexanderlindhorst.riak.session.manager.PersistableSession;
import de.alexanderlindhorst.riak.session.manager.RiakSessionManager;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.alexanderlindhorst.tomcat.session.valve.RequestUtils.getSessionIdFromRequest;
import static de.alexanderlindhorst.tomcat.session.valve.RequestUtils.getSessionIdInternalFromRequest;
import static de.alexanderlindhorst.tomcat.session.valve.RequestUtils.getSessionJvmRouteFromRequest;

/**
 * @author lindhrst (original author)
 */
public class AdjustSessionIdToJvmRouteValve extends ValveBase {

    private static final String ORIGINAL_ID_ATTRIBUTE = "org.apache.catalina.ha.session.JvmRouteOrignalSessionID";

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        //not a known manager -> no optimization
        Manager m = request.getContext().getManager();
        if (!(m instanceof RiakSessionManager)) {
            getNext().invoke(request, response);
            return;
        }

        //no route set on manager, no optimization
        RiakSessionManager manager = (RiakSessionManager) m;
        String routeFromManager = manager.getJvmRoute();
        if (isNullOrEmpty(routeFromManager)) {
            getNext().invoke(request, response);
            return;
        }

        //no session id in request -> no optimization
        String requestSessionId = getSessionIdFromRequest(request);
        if (isNullOrEmpty(requestSessionId)) {
            getNext().invoke(request, response);
            return;
        }

        /*
         Idea:
         Figure out jvmRoute in request URL. If it differs from jvmRoute of manager
         Try to fetch session through manager using old jvmRoute (without new flag)
         If != null, set cookie in response with updated session id (inkl new jvm route)
         */
        String routeFromRequest = getSessionJvmRouteFromRequest(request);
        Request targetRequest = request;
        if (!routeFromManager.equals(routeFromRequest)) {
            String sessionIdInternal = getSessionIdInternalFromRequest(request);
            String localizedNewSessionId = sessionIdInternal + "." + routeFromManager;
            String originalSessionId = request.getSession().getId();
            //retrieves session and adds it to local cache
            PersistableSession sessionFromPersistenceLayer = (PersistableSession) m.findSession(requestSessionId);
            sessionFromPersistenceLayer.setId(localizedNewSessionId);
            request.changeSessionId(localizedNewSessionId);//change request (org.apache.catalina.connector)
            //change request attribute
            request.setAttribute(ORIGINAL_ID_ATTRIBUTE, manager);
            manager.add(sessionFromPersistenceLayer);
            //targetRequest = (Request) create(Request.class, new RequestProxy(request, requestSessionId));
        }
        /*
         Update jvm route in request and pass on
         */
        getNext().invoke(request, response);
    }
}
