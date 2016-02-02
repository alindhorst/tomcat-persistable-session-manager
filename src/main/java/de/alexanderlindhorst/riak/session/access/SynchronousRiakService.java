/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.access;

import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.basho.riak.client.core.RiakNode;

import de.alexanderlindhorst.riak.session.manager.RiakSession;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author alindhorst
 */
public class SynchronousRiakService extends RiakServiceBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronousRiakService.class);
    private RiakNode node;

    @Override
    protected void persistSessionInternal(String sessionId, RiakSession session) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected RiakSession getSessionInternal(String sessionId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void deleteSessionInternal(String sessionId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void init() {
        Pattern addressPattern = Pattern.compile("^(?<host>[^:]+)(:(?<port>.+))?");
        Matcher matcher = addressPattern.matcher(getBackendAddress());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("backend address value " + getBackendAddress() + " cannot be read");
        }
        String host = matcher.group("host");
        String portValue = matcher.group("port");
        int port = 10017;
        if (!isNullOrEmpty(portValue)) {
            port = Integer.valueOf(portValue);
        }
        try {
            node = new RiakNode.Builder()
                    .withRemoteAddress(host)
                    .withRemotePort(port)
                    .build();
        } catch (UnknownHostException ex) {
            throw new IllegalStateException("Couldn't configure riak access", ex);
        }
    }
}
