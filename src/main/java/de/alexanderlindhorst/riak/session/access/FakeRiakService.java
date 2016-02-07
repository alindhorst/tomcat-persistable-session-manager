/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.access;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.alexanderlindhorst.riak.session.manager.BackendServiceBase;

import static com.google.common.collect.Maps.newHashMap;

/**
 * @author alindhorst
 */
public class FakeRiakService extends BackendServiceBase {

    private static final Logger LOGGER = LoggerFactory.getLogger("FakeRiakService");
    private final Map<String, byte[]> sessionStore = newHashMap();

    @Override
    protected void persistSessionInternal(String sessionId, byte[] bytes) {
        LOGGER.debug("Call to persistSessionInternal for session {}", sessionId);
        sessionStore.put(sessionId, bytes);
    }

    @Override
    protected byte[] getSessionInternal(String sessionId) {
        LOGGER.debug("Call to getSessionInternal for id {}", sessionId);
        return sessionStore.get(sessionId);
    }

    @Override
    protected void deleteSessionInternal(String sessionId) {
        LOGGER.debug("Call to deleteSessionInternal for session id {}", sessionId);
        sessionStore.remove(sessionId);
    }

    @Override
    public void init() {
        LOGGER.debug("init");
    }

    @Override
    public void shutdown() {
        LOGGER.debug("This implementation has no tasks during shutdown");
    }
}
