/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.manager;

import java.io.IOException;

import org.apache.catalina.*;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.session.StandardSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.alexanderlindhorst.riak.session.manager.PersistableSession.SESSION_ATTRIBUTE_SET;
import static de.alexanderlindhorst.riak.session.manager.PersistableSession.calculateJvmRoute;
import static de.alexanderlindhorst.riak.session.manager.PersistableSession.calculateJvmRouteAgnosticSessionId;
import static org.apache.catalina.Session.SESSION_CREATED_EVENT;
import static org.apache.catalina.Session.SESSION_DESTROYED_EVENT;

/**
 *
 * @author alindhorst
 */
public class RiakSessionManager extends ManagerBase implements SessionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("SessionManagement");
    private BackendService backendService;
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
            backendService = (BackendService) Class.forName(serviceImplementationClassName).newInstance();
            backendService.setBackendAddress(serviceBackendAddress);
            backendService.init();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new LifecycleException(ex);
        }
    }

    @Override
    protected StandardSession getNewSession() {
        LOGGER.debug("getNewSession");
        PersistableSession session = new PersistableSession(this);
        session.setNew(true);
        session.setValid(true);
        return session;
    }

    @Override
    public Session createSession(String sessionId) {
        LOGGER.debug("createSession {}", sessionId);
        PersistableSession session = (PersistableSession) super.createSession(sessionId);
        session.setDirty(true);
        backendService.persistSession(session);
        session.addSessionListener(this);
        return session;
    }

    private boolean needsRefresh(String idRoute, String contextRoute) {
        if (isNullOrEmpty(idRoute)) {
            return true; //no route in session, refresh every time
        }
        if (!idRoute.equals(contextRoute)) {
            return true; //route in id, but not what we need, refresh
        }
        //id route and context route are the same
        return false;
    }

    @Override
    public Session findSession(String id) throws IOException {
        LOGGER.debug("findSession #{}", id);
        if (isNullOrEmpty(id)) {
            throw new IllegalArgumentException("id must not be null or empty");
        }
        String idJvmRoute = calculateJvmRoute(id);
        String contextJvmRoute = getJvmRoute();
        boolean needsRefresh = needsRefresh(idJvmRoute, contextJvmRoute);
        String jvmRouteAgnosticSessionId = calculateJvmRouteAgnosticSessionId(id);
        PersistableSession session;
        if (!needsRefresh) {
            LOGGER.debug("session id has current jvm route, fetching from local storage");
            session = (PersistableSession) super.findSession(jvmRouteAgnosticSessionId);
        } else {
            LOGGER.debug("session {} has no or not current jvm route, fetching from service for agnostic id {}", id,
                    jvmRouteAgnosticSessionId);
            session = getSessionShell(id);
            session = backendService.getSession(session, jvmRouteAgnosticSessionId);
            if (session != null) {
                LOGGER.debug("session found, setting flags");
                //reinitialize transient fields
                session.setManager(this);
                session.setValid(true);
                addSessionListenerUniquelyTo(session);
                String newId = null;
                if (!(isNullOrEmpty(idJvmRoute) || isNullOrEmpty(contextJvmRoute))) {
                    newId = jvmRouteAgnosticSessionId + "." + contextJvmRoute;
                } else {
                    newId = jvmRouteAgnosticSessionId;
                }
                //overwrite with newly created version
                if (session.isNew()) {
                    fireSessionCreated(session);
                }
                LOGGER.debug("setting session id to new id {}", newId);
                changeSessionId(session, newId);
                add(session);
            }
        }
        return session;
    }

    private PersistableSession getSessionShell(String id) throws IOException {
        PersistableSession session;
        session = (PersistableSession) super.findSession(id);
        if (session != null) {
            //rethink removal
            super.remove(session);
            session.recycle();
            session.setNew(false);
        } else {
            session = new PersistableSession(this);
            session.setNew(true);
        }
        session.setValid(true);
        return session;
    }

    private void fireSessionCreated(Session session) {
        SessionEvent event = new SessionEvent(session, SESSION_CREATED_EVENT, null);
        Object[] applicationEventListeners = getContext().getApplicationEventListeners();
        if (applicationEventListeners == null) {
            return;
        }
        for (int i = 0; i < applicationEventListeners.length; i++) {
            Object applicationEventListener = applicationEventListeners[i];
            if (applicationEventListener instanceof SessionListener) {
                SessionListener listener = (SessionListener) applicationEventListener;
                listener.sessionEvent(event);
            }
        }
    }

    private void addSessionListenerUniquelyTo(PersistableSession session) {
        //remove if added previously
        session.removeSessionListener(this);
        //and add again
        session.addSessionListener(this);
    }

    public void storeSession(PersistableSession session) {
        LOGGER.debug("storeSession called for id {}", session != null ? session.getId() : "[session is null]");
        if (session == null || !session.isDirty()) {
            LOGGER.debug("no persisting needed, will return");
            return;
        }
        LOGGER.debug("storing session");
        backendService.persistSession(session);
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
        PersistableSession session = (PersistableSession) event.getSession();
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
        backendService.deleteSession((PersistableSession) session);
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

    @Override
    protected void destroyInternal() throws LifecycleException {
        LOGGER.debug("destroyInternal");
        if (backendService != null) {
            backendService.shutdown();
            LOGGER.debug("backend service shutdown seems to have completed");
        }
        super.destroyInternal();
    }
}
