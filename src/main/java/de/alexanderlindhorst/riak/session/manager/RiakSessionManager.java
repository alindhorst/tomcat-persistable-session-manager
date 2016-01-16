package de.alexanderlindhorst.riak.session.manager;

import java.io.IOException;

import org.apache.catalina.Session;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.session.StandardSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.alexanderlindhorst.riak.session.access.RiakService;

/**
 *
 * @author alindhorst
 */
public class RiakSessionManager extends ManagerBase {

    private static final Logger LOGGER = LoggerFactory.getLogger("SessionManagement");
    private RiakService riakService;
    
    @Override
    protected StandardSession getNewSession() {
        LOGGER.debug("getNewSession");
        RiakSession session=new RiakSession(this);
        session.setNew(true);
        session.setValid(true);
        return session;
    }

    @Override
    public Session createSession(String sessionId) {
        LOGGER.debug("createSession {}", sessionId);
        return super.createSession(sessionId); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Session createEmptySession() {
        LOGGER.debug("createEmptySession");
        return super.createEmptySession(); //To change body of generated methods, choose Tools | Templates.
    }


    @Override
    public void load() throws ClassNotFoundException, IOException {
    }

    @Override
    public void unload() throws IOException {
    }

}
