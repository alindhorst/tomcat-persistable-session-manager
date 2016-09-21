/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.alexanderlindhorst.tomcat.session.manager.PersistableSessionUtils.deserializeSessionInto;
import static de.alexanderlindhorst.tomcat.session.manager.PersistableSessionUtils.serializeSession;

/**
 * @author alindhorst
 */
public abstract class BackendServiceBase implements BackendService {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BackendServiceBase.class);
    private String backendAddress;

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
}
