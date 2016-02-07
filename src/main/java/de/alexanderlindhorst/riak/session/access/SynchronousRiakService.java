/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.access;

import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.RiakObject;
import com.basho.riak.client.core.util.BinaryValue;

import de.alexanderlindhorst.riak.session.manager.BackendServiceBase;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author alindhorst
 */
public class SynchronousRiakService extends BackendServiceBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronousRiakService.class);
    private static final Namespace SESSIONS = new Namespace("SESSIONS");
    private RiakCluster cluster;

    @Override
    protected void persistSessionInternal(String sessionId, byte[] bytes) {
        LOGGER.debug("persistSessionInternal {}", sessionId);
        try {
            RiakClient client = new RiakClient(cluster);
            RiakObject object = new RiakObject().setValue(BinaryValue.create(bytes));
            Location location = new Location(SESSIONS, sessionId);
            StoreValue storeOp = new StoreValue.Builder(object)
                    .withLocation(location)
                    .build();
            StoreValue.Response response = client.execute(storeOp);
            LOGGER.debug("persistSessionInternal - Response: {}", response);
        } catch (ExecutionException | InterruptedException exception) {
            throw new RiakAccessException("Couldn't persist session", exception);
        }
    }

    @Override
    protected byte[] getSessionInternal(String sessionId) {
        try {
            LOGGER.debug("getSessionInternal {}", sessionId);
            RiakClient client = new RiakClient(cluster);
            Location location = new Location(SESSIONS, sessionId);
            FetchValue fetchValue = new FetchValue.Builder(location).build();
            return client.execute(fetchValue).getValue(RiakObject.class).getValue().getValue();
        } catch (ExecutionException | InterruptedException ex) {
            throw new RiakAccessException("Couldn't fetch session " + sessionId, ex);
        }
    }

    @Override
    protected void deleteSessionInternal(String sessionId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void init() {
        if (isNullOrEmpty(getBackendAddress())) {
            throw new IllegalArgumentException("backend address must not be null or empty");
        }
        Pattern addressPattern = Pattern.compile("^(?<host>[^:]+)(:(?<port>\\d+))?");
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
            RiakNode node = new RiakNode.Builder()
                    .withRemoteAddress(host)
                    .withRemotePort(port)
                    .build();
            cluster = new RiakCluster.Builder(node)
                    .build();
            cluster.start();
        } catch (UnknownHostException ex) {
            throw new IllegalStateException("Couldn't configure riak access", ex);
        }
    }
}
