/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

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
                backendService = type.newInstance();
            } catch (IllegalAccessException | InstantiationException ex) {
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
            this.type = (Class<TYPE>) Class.forName(typeClassName);
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
        return endpointDelegates.stream().findAny().get().getSession(emptyShell, id);
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
