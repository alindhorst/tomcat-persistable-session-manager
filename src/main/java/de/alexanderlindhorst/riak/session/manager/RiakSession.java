package de.alexanderlindhorst.riak.session.manager;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;

/**
 * @author alindhorst
 */
public class RiakSession extends StandardSession {

    public static final String SESSION_ATTRIBUTE_SET = "SESSION_ATTRIBUTE_SET";
    private transient boolean dirty = false;

    public RiakSession(Manager manager) {
        super(manager);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        fireSessionAttributeSet(new PersistableSessionAttribute(name, value));
    }

    private void fireSessionAttributeSet(PersistableSessionAttribute sessionAttribute) {
        fireSessionEvent(SESSION_ATTRIBUTE_SET, sessionAttribute);
    }
}
