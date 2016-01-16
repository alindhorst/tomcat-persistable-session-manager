
package de.alexanderlindhorst.riak.session.access;

import de.alexanderlindhorst.riak.session.manager.RiakSession;

/**
 * @author alindhorst
 */
public class FakeRiakService implements RiakService{

    @Override
    public void persistSession(RiakSession session) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RiakSession getSession(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteSession(RiakSession session) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
