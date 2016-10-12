/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.manager;

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

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.alexanderlindhorst.tomcat.session.manager.PersistableSession.SESSION_ATTRIBUTE_SET;
import static de.alexanderlindhorst.tomcat.session.manager.PersistableSessionUtils.calculateJvmRoute;
import static de.alexanderlindhorst.tomcat.session.manager.PersistableSessionUtils.calculateJvmRouteAgnosticSessionId;
import static java.lang.System.currentTimeMillis;
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
    private long sessionExpiryThreshold = -1;

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

    public long getSessionExpiryThreshold() {
        return sessionExpiryThreshold;
    }

    public void setSessionExpiryThreshold(long serviceSessionExpiryThreshold) {
        this.sessionExpiryThreshold = serviceSessionExpiryThreshold;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        LOGGER.debug("initInternal called");
        super.initInternal();
        try {
            backendService = (BackendService) Class.forName(serviceImplementationClassName).newInstance();
            backendService.setBackendAddress(serviceBackendAddress);
            backendService.setSessionManagementLogger(LOGGER);
            backendService.setSessionExpiryThreshold(sessionExpiryThreshold);
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
        add(session);
        fireSessionCreated(session);
        return session;
    }

    private boolean needsRefresh(String idRoute, String contextRoute) {
        if (isNullOrEmpty(idRoute)) {
            return true; //no route in session, refresh every time
        }
        if (isNullOrEmpty(contextRoute)) {
            return true; //no route in context, refresh every time
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
            session = (PersistableSession) super.findSession(id);
        } else {
            String newId;
            if (!isNullOrEmpty(contextJvmRoute)) {
                newId = jvmRouteAgnosticSessionId + "." + contextJvmRoute;
            } else {
                newId = jvmRouteAgnosticSessionId;
            }
            LOGGER.debug("session {} has no or not current jvm route, fetching from service for agnostic id {}", id,
                    jvmRouteAgnosticSessionId);
            session = backendService.getSession(getSessionShell(), jvmRouteAgnosticSessionId);
            if (session == null) {
                LOGGER.warn("Creating a new session for passed in id {} as nothing could be found in the backend.\n"
                        + "This might be an exploitation attempt.");
                session = (PersistableSession) createSession(newId);
            } else {
                LOGGER.debug("session found, setting flags");
                //reinitialize transient fields
                session.setManager(this);
                addSessionListenerUniquelyTo(session);

                LOGGER.debug("setting session id to new id {}", newId);
                changeSessionId(session, newId);
                add(session);
            }
        }
        session.touchLastAccessedTime();
        return session;
    }

    private PersistableSession getSessionShell() throws IOException {
        PersistableSession session = new PersistableSession(this);
        session.setNew(true);
        session.setValid(true);
        return session;
    }

    private void fireSessionCreated(Session session) {
        SessionEvent event = new SessionEvent(session, SESSION_CREATED_EVENT, null);
        Object[] applicationEventListeners = getContext().getApplicationEventListeners();
        if (applicationEventListeners == null) {
            return;
        }
        for (Object applicationEventListener : applicationEventListeners) {
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

    @Override
    public void processExpires() {
        if (backendService != null) {
            LOGGER.debug("removing expired sessions");

            final String idSuffix;
            if (!isNullOrEmpty(getJvmRoute())) {
                idSuffix = "." + getJvmRoute();
            } else {
                idSuffix = "";
            }

            final long removalThreshold = currentTimeMillis() - getSessionExpiryThreshold();

            backendService.removeExpiredSessions().forEach(id -> {
                try {
                    PersistableSession session = (PersistableSession) super.findSession(id + idSuffix);
                    if (session != null) {
                        if (session.getLastAccessedTime() < removalThreshold) {
                            super.remove(session);
                        } else {
                            //locally newer than remote, write back
                            session.setDirty(true);
                            storeSession(session);
                        }
                    }
                } catch (IOException ex) {
                    LOGGER.error("Couldn't find session to remove it locally: " + id, ex);
                }
            });
        } else {
            throw new IllegalStateException("No backend service found, can't process expired sessions");
        }
    }

}
