/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
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
