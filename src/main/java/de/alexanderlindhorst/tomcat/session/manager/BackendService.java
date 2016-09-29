/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.manager;

import java.util.List;

/**
 * @author alindhorst
 */
public interface BackendService {

    public void persistSession(PersistableSession session);

    public List<String> removeExpiredSessions();

    public PersistableSession getSession(PersistableSession emptyShell, String id);

    public void deleteSession(PersistableSession session);

    public void setBackendAddress(String backendAddress);

    public List<String> getExpiredSessionIds();

    public void init();

    void shutdown();
}
