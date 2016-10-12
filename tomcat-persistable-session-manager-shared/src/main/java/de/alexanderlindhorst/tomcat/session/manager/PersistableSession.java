/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.manager;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;

import static de.alexanderlindhorst.tomcat.session.manager.PersistableSessionUtils.calculateJvmRouteAgnosticSessionId;

/**
 * @author alindhorst
 */
public class PersistableSession extends StandardSession {

    public static final String SESSION_ATTRIBUTE_SET = "SESSION_ATTRIBUTE_SET";
    private static final long serialVersionUID = 2L;
    private transient boolean dirty = false;
    private transient long lastAccessedLocally = System.currentTimeMillis();

    public PersistableSession(Manager manager) {
        super(manager);
    }

    public String getPersistenceKey() {
        return calculateJvmRouteAgnosticSessionId(getId());
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

    @Override
    public long getLastAccessedTime() {
        return lastAccessedLocally;
    }

    public void touchLastAccessedTime() {
        lastAccessedLocally = System.currentTimeMillis();
    }
}
