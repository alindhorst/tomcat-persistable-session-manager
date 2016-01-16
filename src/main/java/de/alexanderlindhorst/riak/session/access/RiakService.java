
package de.alexanderlindhorst.riak.session.access;

import de.alexanderlindhorst.riak.session.manager.RiakSession;

/**
  * @author alindhorst
  */
public interface RiakService {
    public void persistSession(RiakSession session);
    public RiakSession getSession(String id);
    public void deleteSession(RiakSession session);
}
