/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.access;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.alexanderlindhorst.tomcat.session.manager.BackendServiceBase;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * @author alindhorst
 */
public class FakeBackendService extends BackendServiceBase {

    private final Map<String, byte[]> sessionStore = newHashMap();
    private final Map<String, Long> lastAccessed = newHashMap();

    @Override
    protected void persistSessionInternal(String sessionId, byte[] bytes) {
        LOGGER.debug("Call to persistSessionInternal for session {}", sessionId);
        sessionStore.put(sessionId, bytes);
        lastAccessed.put(sessionId, currentTimeMillis());
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
        super.init();
        LOGGER.debug("init");
    }

    @Override
    public void shutdown() {
        super.shutdown();
        LOGGER.debug("This implementation has no tasks during shutdown");
    }

    @Override
    public List<String> removeExpiredSessions() {
        List<String> expiredSessionIds = getExpiredSessionIds();
        expiredSessionIds.forEach(id -> {
            sessionStore.remove(id);
            lastAccessed.remove(id);
        });
        return expiredSessionIds;
    }

    @Override
    public List<String> getExpiredSessionIds() {
        if (getSessionExpiryThreshold() == -1) {
            return Collections.<String>emptyList();
        }
        List<String> ids = lastAccessed.entrySet().stream()
                .filter(entry -> entry.getValue() < currentTimeMillis() - getSessionExpiryThreshold())
                .map(entry -> entry.getKey())
                .collect(toList());
        return ids;
    }
}
