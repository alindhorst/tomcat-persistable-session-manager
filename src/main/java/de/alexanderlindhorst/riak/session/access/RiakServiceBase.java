package de.alexanderlindhorst.riak.session.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.alexanderlindhorst.riak.session.manager.RiakSession;

import static de.alexanderlindhorst.riak.session.manager.RiakSession.calculateJvmRouteAgnosticSessionId;

/**
 * @author alindhorst
 */
abstract class RiakServiceBase implements RiakService {

    protected static final Logger LOGGER = LoggerFactory.getLogger("RiakService");

    @Override
    public final void persistSession(RiakSession session) {
        persistSessionInternal(session.getIdInternal(), session);
    }

    protected abstract void persistSessionInternal(String sessionId, RiakSession session);

    @Override
    public final RiakSession getSession(String id) {
        return getSessionInternal(calculateJvmRouteAgnosticSessionId(id));
    }

    protected abstract RiakSession getSessionInternal(String sessionId);

    @Override
    public final void deleteSession(RiakSession session) {
        deleteSessionInternal(session.getIdInternal());
    }

    protected abstract void deleteSessionInternal(String sessionId);
}
