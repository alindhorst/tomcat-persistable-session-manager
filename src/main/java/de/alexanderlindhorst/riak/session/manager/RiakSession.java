/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.manager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;

/**
 * @author alindhorst
 */
public class RiakSession extends StandardSession {

    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^(?<sessionId>[^\\.]+)(\\.(?<jvmRoute>.*))?$");
    public static final String SESSION_ATTRIBUTE_SET = "SESSION_ATTRIBUTE_SET";
    private transient boolean dirty = false;

    public RiakSession(Manager manager) {
        super(manager);
    }

    @Override
    public String getIdInternal() {
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
        Matcher matcher = SESSION_ID_PATTERN.matcher(id);
        matcher.find();
        return matcher.group("sessionId");
    }

    public static String calculateJvmRoute(String id) {
        Matcher matcher = SESSION_ID_PATTERN.matcher(id);
        matcher.find();
        return matcher.group("jvmRoute");
    }
}
