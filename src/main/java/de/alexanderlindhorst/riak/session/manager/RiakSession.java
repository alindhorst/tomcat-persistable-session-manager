package de.alexanderlindhorst.riak.session.manager;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;

/**
 * @author alindhorst
 */
public class RiakSession extends StandardSession {
    private transient boolean dirty=false;

    public RiakSession(Manager manager) {
        super(manager);
    }

    public boolean isDirty() {
        return dirty;
    }

}
