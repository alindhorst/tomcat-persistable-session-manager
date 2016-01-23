package de.alexanderlindhorst.riak.session.manager;

import java.io.IOException;

import org.apache.catalina.LifecycleException;
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
    private String riakServiceImplementationClassName;

    public String getRiakServiceImplementationClassName() {
        return riakServiceImplementationClassName;
    }

    public void setRiakServiceImplementationClassName(String riakServiceImplementationClassName) {
        this.riakServiceImplementationClassName = riakServiceImplementationClassName;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        try {
            riakService = (RiakService) Class.forName(riakServiceImplementationClassName).newInstance();
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
        return session;
    }

    @Override
    public Session findSession(String id) throws IOException {
        String idJvmRoute = calculateJvmRoute(id);
        String contextJvmRoute = getJvmRoute();
        RiakSession session;
        if (idJvmRoute != null && idJvmRoute.equals(contextJvmRoute)) {
            session = (RiakSession) super.findSession(calculateJvmRouteAgnosticSessionId(id));
        } else {
            session = riakService.getSession(calculateJvmRouteAgnosticSessionId(id));
            if (session != null) {
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

    public void storeSession(RiakSession session) {
        if (!session.isDirty()) {
            return;
        }
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
}
