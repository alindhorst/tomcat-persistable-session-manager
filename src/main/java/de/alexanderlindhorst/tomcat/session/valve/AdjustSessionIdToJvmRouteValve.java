/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.valve;

import java.io.IOException;

import org.apache.catalina.Manager;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import de.alexanderlindhorst.riak.session.manager.RiakSessionManager;

import javax.servlet.ServletException;

import static de.alexanderlindhorst.tomcat.session.valve.RequestUtils.getSessionJvmRouteFromRequest;

/**
 * @author lindhrst (original author)
 */
public class AdjustSessionIdToJvmRouteValve extends ValveBase {

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        Manager m = request.getContext().getManager();
        if (!(m instanceof RiakSessionManager)) {
            getNext().invoke(request, response);
            return;
        }
        /*
        Idea:
        Figure out jvmRoute in request URL. If it differs from jvmRoute of manager
        Try to fetch session through manager using old jvmRoute (without new flag)
        If != null, set cookie in response with updated session id (inkl new jvm route)
         */
        RiakSessionManager manager = (RiakSessionManager) m;
        manager.getJvmRoute();
        String routeFromRequest = getSessionJvmRouteFromRequest(request);
        /*
        Update jvm route in request and pass on
         */
        getNext().invoke(request, response);
    }
}
