/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.alexanderlindhorst.riak.session.manager.PersistableSession.calculateJvmRouteAgnosticSessionId;
import static de.alexanderlindhorst.riak.session.manager.PersistableSessionUtils.deserializeSessionInto;
import static de.alexanderlindhorst.riak.session.manager.PersistableSessionUtils.serializeSession;

/**
 * @author alindhorst
 */
public abstract class BackendServiceBase implements BackendService {

    protected static final Logger LOGGER = LoggerFactory.getLogger("RiakService");
    private String backendAddress;

    @Override
    public final void persistSession(PersistableSession session) {
        persistSessionInternal(session.getIdInternal(), serializeSession(session));
        session.setDirty(false);
    }

    protected abstract void persistSessionInternal(String sessionId, byte[] bytes);

    @Override
    public final PersistableSession getSession(PersistableSession emptyShell, String id) {
        return deserializeSessionInto(emptyShell, getSessionInternal(calculateJvmRouteAgnosticSessionId(id)));
    }

    protected abstract byte[] getSessionInternal(String sessionId);

    @Override
    public final void deleteSession(PersistableSession session) {
        deleteSessionInternal(session.getIdInternal());
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
