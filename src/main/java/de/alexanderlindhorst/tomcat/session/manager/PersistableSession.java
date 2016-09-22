/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.manager;

import java.util.regex.Matcher;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;

/**
 * @author alindhorst
 */
public class PersistableSession extends StandardSession {

    public static final String SESSION_ATTRIBUTE_SET = "SESSION_ATTRIBUTE_SET";
    private static final long serialVersionUID = 1L;
    private transient boolean dirty = false;

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

    public static String calculateJvmRouteAgnosticSessionId(String id) {
        Matcher matcher = PersistableSessionUtils.SESSION_ID_PATTERN.matcher(id);
        matcher.find();
        return matcher.group("sessionId");
    }

    public static String calculateJvmRoute(String id) {
        Matcher matcher = PersistableSessionUtils.SESSION_ID_PATTERN.matcher(id);
        matcher.find();
        return matcher.group("jvmRoute");
    }
}
