/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.manager;

/**
 * @author alindhorst
 */
public interface BackendService {

    public void persistSession(PersistableSession session);

    public PersistableSession getSession(PersistableSession emptyShell, String id);

    public void deleteSession(PersistableSession session);

    public void setBackendAddress(String backendAddress);

    public void init();

    void shutdown();
}
