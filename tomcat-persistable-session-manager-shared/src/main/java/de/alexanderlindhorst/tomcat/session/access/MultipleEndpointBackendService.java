/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import de.alexanderlindhorst.tomcat.session.manager.PersistableSession;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

/**
 * This service delegates lifecycle calls to an internally kept list of {@link BackendService} instances, for example in different data
 * centers.
 *
 * @author alindhorst
 */
public class MultipleEndpointBackendService<TYPE extends BackendService> implements BackendService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleEndpointBackendService.class);
    private List<TYPE> endpointDelegates;
    private Class<TYPE> type;
    private String backendAddress;
    private long sessionExpiryThreshold = -1l;
    private Logger sessionManagementLogger = null;

    @Override
    public final void setBackendAddress(String backendAddress) {
        this.backendAddress = backendAddress;
    }

    public final String getBackendAddress() {
        return backendAddress;
    }

    @Override
    public void init() {
        if (type == null) {
            throw new IllegalArgumentException("backend service type not set");
        }
        if (isNullOrEmpty(getBackendAddress())) {
            throw new IllegalArgumentException("backend address must not be null or empty");
        }
        endpointDelegates = Lists.newArrayList();
        ArrayList<String> splitValues = newArrayList(getBackendAddress().split(";"));
        splitValues.forEach(value -> {
            TYPE backendService = null;
            try {
                backendService = type.getDeclaredConstructor().newInstance();
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException
                    | java.lang.reflect.InvocationTargetException ex) {
                throw new IllegalStateException(ex);
            }
            backendService.setBackendAddress(value);
            backendService.setSessionExpiryThreshold(sessionExpiryThreshold);
            backendService.setSessionManagementLogger(sessionManagementLogger);
            backendService.init();
            endpointDelegates.add(backendService);
        });
    }

    @SuppressWarnings("unchecked")
    public void setBackendServiceType(String typeClassName) {
        try {
            Class<?> clazz = Class.forName(typeClassName);
            if (!BackendService.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(typeClassName + " does not implement BackendService");
            }
            this.type = (Class<TYPE>) clazz;
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public List<String> removeExpiredSessions() {
        Set<String> ids = newHashSet();
        endpointDelegates.forEach(endpoint -> ids.addAll(endpoint.removeExpiredSessions()));
        return newArrayList(ids);
    }

    @Override
    public List<String> getExpiredSessionIds() {
        Set<String> ids = newHashSet();
        endpointDelegates.forEach(endpoint -> ids.addAll(endpoint.getExpiredSessionIds()));
        return newArrayList(ids);
    }

    @Override
    public void persistSession(PersistableSession session) {
        endpointDelegates.forEach(delegate -> delegate.persistSession(session));
    }

    @Override
    public PersistableSession getSession(PersistableSession emptyShell, String id) {
        RuntimeException lastException = null;
        for (TYPE delegate : endpointDelegates) {
            try {
                PersistableSession session = delegate.getSession(emptyShell, id);
                if (session != null) {
                    return session;
                }
            } catch (RuntimeException e) {
                LOGGER.warn("Delegate {} failed to retrieve session {}, trying next", delegate, id, e);
                lastException = e;
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        return null;
    }

    @Override
    public void deleteSession(PersistableSession session) {
        endpointDelegates.forEach(delegate -> delegate.deleteSession(session));
    }

    @Override
    public void shutdown() {
        endpointDelegates.forEach(delegate -> delegate.shutdown());
    }

    @Override
    public void setSessionExpiryThreshold(long sessionExpiryThresholdMilliSeconds) {
        this.sessionExpiryThreshold = sessionExpiryThresholdMilliSeconds;
    }

    @Override
    public void setSessionManagementLogger(Logger sessionManagementLogger) {
        this.sessionManagementLogger = sessionManagementLogger;
    }

}
