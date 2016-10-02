/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.manager;

import java.util.List;

import org.slf4j.Logger;

/**
 * @author alindhorst
 */
public interface BackendService {

    void persistSession(PersistableSession session);

    List<String> removeExpiredSessions();

    PersistableSession getSession(PersistableSession emptyShell, String id);

    void deleteSession(PersistableSession session);

    void setBackendAddress(String backendAddress);

    List<String> getExpiredSessionIds();

    void init();

    void shutdown();

    void setCleanUpRunIntervalSeconds(long cleanUpRunIntervalSeconds);

    void setSessionExpiryThreshold(long sessionExpiryThresholdMilliSeconds);

    void setSessionManagementLogger(Logger sessionManagementLogger);
}
