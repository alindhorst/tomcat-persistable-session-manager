package de.alexanderlindhorst.riak.session.access;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.alexanderlindhorst.riak.session.manager.RiakSession;

import static com.google.common.collect.Maps.newHashMap;

/**
 * @author alindhorst
 */
public class FakeRiakService extends RiakServiceBase {

    private static final Logger LOGGER = LoggerFactory.getLogger("FakeRiakService");
    private final Map<String, RiakSession> sessionStore = newHashMap();

    @Override
    protected void persistSessionInternal(String sessionId, RiakSession session) {
        sessionStore.put(session.getIdInternal(), session);
        LOGGER.debug("Call to persistSessionInternal for session {}", sessionId);
    }

    @Override
    protected RiakSession getSessionInternal(String sessionId) {
        LOGGER.debug("Call to getSessionInternal for id {}", sessionId);
        return sessionStore.get(sessionId);
    }

    @Override
    protected void deleteSessionInternal(String sessionId) {
        LOGGER.debug("Call to deleteSessionInternal for session id {}", sessionId);
        sessionStore.remove(sessionId);
    }

}
