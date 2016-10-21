/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.access;

import de.alexanderlindhorst.tomcat.session.manager.PersistableSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.alexanderlindhorst.tomcat.session.manager.PersistableSessionUtils.deserializeSessionInto;
import static de.alexanderlindhorst.tomcat.session.manager.PersistableSessionUtils.serializeSession;

/**
 * @author alindhorst
 */
public abstract class BackendServiceBase implements BackendService {

    public static final long SESSIONS_NEVER_EXPIRE = -1;
    protected static final Logger LOGGER = LoggerFactory.getLogger(BackendServiceBase.class);
    private Logger sessionManagementLogger = LOGGER;
    private String backendAddress;
    private long sessionExpiryThreshold = SESSIONS_NEVER_EXPIRE;
    private boolean shuttingDown;

    @Override
    public void init() {
        shuttingDown = false;
    }

    @Override
    public void shutdown() {
        shuttingDown = true;
    }

    @Override
    public final void persistSession(PersistableSession session) {
        persistSessionInternal(session.getPersistenceKey(), serializeSession(session));
        session.setDirty(false);
    }

    protected abstract void persistSessionInternal(String sessionId, byte[] bytes);

    @Override
    public final PersistableSession getSession(PersistableSession emptyShell, String id) {
        return deserializeSessionInto(emptyShell, getSessionInternal(id));
    }

    protected abstract byte[] getSessionInternal(String sessionId);

    @Override
    public final void deleteSession(PersistableSession session) {
        deleteSessionInternal(session.getPersistenceKey());
    }

    protected abstract void deleteSessionInternal(String sessionId);

    @Override
    public final void setBackendAddress(String backendAddress) {
        this.backendAddress = backendAddress;
    }

    public final String getBackendAddress() {
        return backendAddress;
    }

    @Override
    public final void setSessionExpiryThreshold(long sessionExpiryThresholdMilliSeconds) {
        this.sessionExpiryThreshold = sessionExpiryThresholdMilliSeconds;
    }

    public final long getSessionExpiryThreshold() {
        return sessionExpiryThreshold;
    }

    public final Logger getSessionManagementLogger() {
        return sessionManagementLogger;
    }

    @Override
    public final void setSessionManagementLogger(Logger sessionManagementLogger) {
        this.sessionManagementLogger = sessionManagementLogger;
    }

    protected final boolean isShuttingDown() {
        return shuttingDown;
    }
}
