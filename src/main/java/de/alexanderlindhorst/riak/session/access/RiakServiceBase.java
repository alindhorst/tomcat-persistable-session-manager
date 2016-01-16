package de.alexanderlindhorst.riak.session.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.alexanderlindhorst.riak.session.manager.RiakSession;

/**
 * @author alindhorst
 */
abstract class RiakServiceBase implements RiakService {

    protected static final Logger LOGGER = LoggerFactory.getLogger("RiakService");

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
