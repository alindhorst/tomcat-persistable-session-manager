package de.alexanderlindhorst.riak.session.manager;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.apache.catalina.session.ManagerBase;

/**
 *
 * @author alindhorst
 */
public class SessionManager extends ManagerBase {

    private final AtomicInteger rejected = new AtomicInteger(0);

    @Override
    public int getRejectedSessions() {
        return rejected.get();
    }

    @Override
    public void setRejectedSessions(int i) {
        rejected.set(i);
    }

    public void rejectSession() {
        rejected.set(rejected.get()+1);
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {
        //ordered loading is not supported
    }

    @Override
    public void unload() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
