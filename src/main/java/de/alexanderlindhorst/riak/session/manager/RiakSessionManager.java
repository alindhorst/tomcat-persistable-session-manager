package de.alexanderlindhorst.riak.session.manager;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.session.StandardSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.alexanderlindhorst.riak.session.access.RiakService;

/**
 *
 * @author alindhorst
 */
public class RiakSessionManager extends ManagerBase implements SessionListener {

    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^(?<sessionId>[^\\.]+)(\\.(?<jvmRoute>.*))?$");
    private static final Logger LOGGER = LoggerFactory.getLogger("SessionManagement");
    private RiakService riakService;

    @Override
    protected StandardSession getNewSession() {
        LOGGER.debug("getNewSession");
        RiakSession session = new RiakSession(this);
        session.setNew(true);
        session.setValid(true);
        return session;
    }

    @Override
    public Session createSession(String sessionId) {
        LOGGER.debug("createSession {}", sessionId);
        RiakSession session = (RiakSession) super.createSession(sessionId);
        session.setDirty(true);
        riakService.persistSession(session);
        return session;
    }

    @Override
    public Session findSession(String id) throws IOException {
        String idJvmRoute = getJvmRoute(id);
        String contextJvmRoute = getJvmRoute();
        RiakSession session;
        if (idJvmRoute != null && idJvmRoute.equals(contextJvmRoute)) {
            session = (RiakSession) super.findSession(id);
        } else {
            session = riakService.getSession(id);
        }
        return session;
    }

    public void storeSession(RiakSession session) {
        if (!session.isDirty()) {
            return;
        }
        riakService.persistSession(session);
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {
    }

    @Override
    public void unload() throws IOException {
    }

    private static String getJvmRouteAgnosticSessionId(String id) {
        Matcher matcher = SESSION_ID_PATTERN.matcher(id);
        if (matcher.matches()) {
            return matcher.group("sessionId");
        }
        return null;
    }

    private static String getJvmRoute(String id) {
        Matcher matcher = SESSION_ID_PATTERN.matcher(id);
        if (matcher.matches()) {
            return matcher.group("jvmRoute");
        }
        return null;
    }

    @Override
    public void sessionEvent(SessionEvent event) {
        LOGGER.debug("event {}", event);
        RiakSession session = (RiakSession) event.getSession();
        session.setDirty(true);
        riakService.persistSession(session);
    }
}
