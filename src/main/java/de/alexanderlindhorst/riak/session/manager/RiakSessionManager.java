/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.manager;

import java.io.IOException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.session.StandardSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.alexanderlindhorst.riak.session.access.RiakService;

import static de.alexanderlindhorst.riak.session.manager.RiakSession.SESSION_ATTRIBUTE_SET;
import static de.alexanderlindhorst.riak.session.manager.RiakSession.calculateJvmRoute;
import static de.alexanderlindhorst.riak.session.manager.RiakSession.calculateJvmRouteAgnosticSessionId;
import static org.apache.catalina.Session.SESSION_CREATED_EVENT;
import static org.apache.catalina.Session.SESSION_DESTROYED_EVENT;

/**
 *
 * @author alindhorst
 */
public class RiakSessionManager extends ManagerBase implements SessionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("SessionManagement");
    private RiakService riakService;
    private String serviceImplementationClassName;
    private String serviceBackendAddress;

    public String getServiceImplementationClassName() {
        return serviceImplementationClassName;
    }

    public void setServiceImplementationClassName(String riakServiceImplementationClassName) {
        this.serviceImplementationClassName = riakServiceImplementationClassName;
    }

    public String getServiceBackendAddress() {
        return serviceBackendAddress;
    }

    public void setServiceBackendAddress(String serviceBackendAddress) {
        this.serviceBackendAddress = serviceBackendAddress;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        LOGGER.debug("initInternal called");
        super.initInternal();
        try {
            riakService = (RiakService) Class.forName(serviceImplementationClassName).newInstance();
            riakService.setBackendAddress(serviceBackendAddress);
            riakService.init();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new LifecycleException(ex);
        }
    }

    @Override
    protected StandardSession getNewSession() {
        LOGGER.debug("getNewSession");
        RiakSession session = new RiakSession(this);
        session.setNew(true);
        session.setValid(true);
        return session;
    }

    @Override
    public Session createSession(String sessionId) {
        LOGGER.debug("createSession {}", sessionId);
        RiakSession session = (RiakSession) super.createSession(sessionId);
        session.setDirty(true);
        riakService.persistSession(session);
        session.addSessionListener(this);
        return session;
    }

    @Override
    public Session findSession(String id) throws IOException {
        LOGGER.debug("findSession #{}", id);
        String idJvmRoute = calculateJvmRoute(id);
        String contextJvmRoute = getJvmRoute();
        RiakSession session;
        if (idJvmRoute != null && idJvmRoute.equals(contextJvmRoute)) {
            LOGGER.debug("session id has current jvm route, fetching from local storage");
            session = (RiakSession) super.findSession(calculateJvmRouteAgnosticSessionId(id));
        } else {
            LOGGER.debug("session {} has no or not current jvm route, fetching from service", id);
            session = riakService.getSession(calculateJvmRouteAgnosticSessionId(id));
            if (session != null) {
                LOGGER.debug("session found, setting flags");
                //reinitialize transient fields
                session.setManager(this);
                addSessionListenerUniquelyTo(session);
                String oldId = session.getId();
                String newId = session.getIdInternal();
                if (contextJvmRoute != null) {
                    newId = newId + "." + contextJvmRoute;
                }
                if (!oldId.equals(newId)) {
                    session.setId(newId);
                    session.tellChangedSessionId(newId, oldId, true, true);
                }
            }
        }
        return session;
    }

    private void addSessionListenerUniquelyTo(RiakSession session) {
        //remove if added previously
        session.removeSessionListener(this);
        //and add again
        session.addSessionListener(this);
    }

    public void storeSession(RiakSession session) {
        LOGGER.debug("storeSession called for id {}", session != null ? session.getId() : "[session is null]");
        if (session == null || !session.isDirty()) {
            LOGGER.debug("no persisting needed, will return");
            return;
        }
        LOGGER.debug("storing session");
        riakService.persistSession(session);
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {
    }

    @Override
    public void unload() throws IOException {
    }

    @Override
    public void sessionEvent(SessionEvent event) {
        LOGGER.debug("event {}", event);
        RiakSession session = (RiakSession) event.getSession();
        switch (event.getType()) {
            case SESSION_DESTROYED_EVENT:
                remove(session);
                break;
            case SESSION_CREATED_EVENT:
            case SESSION_ATTRIBUTE_SET:
                session.setDirty(true);
                storeSession(session);
                break;
            default:
                throw new AssertionError("Unknown event type: " + event.getType());
        }
    }

    @Override
    public void remove(Session session) {
        super.remove(session);
        riakService.deleteSession((RiakSession) session);
    }

    /* Life cycle stuff */
    @Override
    protected void startInternal() throws LifecycleException {
        LOGGER.debug("startInternal");
        super.startInternal();
        LifecycleState state = getState();
        if (LifecycleState.STARTING_PREP.equals(state)) {
            setState(LifecycleState.STARTING);
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        LOGGER.debug("stopInternal");
        super.stopInternal();
        LifecycleState state = getState();
        if (LifecycleState.STOPPING_PREP.equals(state)) {
            setState(LifecycleState.STOPPING);
        }
    }

}
