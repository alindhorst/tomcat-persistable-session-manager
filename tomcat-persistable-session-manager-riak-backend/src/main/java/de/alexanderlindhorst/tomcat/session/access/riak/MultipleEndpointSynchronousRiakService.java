/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.access.riak;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import de.alexanderlindhorst.tomcat.session.manager.BackendServiceBase;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

/**
 * This service delegates lifecycle calls to an internally kept list of {@link SynchronousRiakService} instances and this supports
 * interacting with several Riak clusters, for example in different data centers.
 *
 * @author alindhorst
 */
public class MultipleEndpointSynchronousRiakService extends BackendServiceBase {

    private List<SynchronousRiakService> endpointDelegates;

    @Override
    public void init() {
        super.init();
        if (isNullOrEmpty(getBackendAddress())) {
            throw new IllegalArgumentException("backend address must not be null or empty");
        }
        endpointDelegates = Lists.newArrayList();
        ArrayList<String> splitValues = newArrayList(getBackendAddress().split(";"));
        splitValues.forEach(value -> {
            SynchronousRiakService riakService = new SynchronousRiakService();
            riakService.setBackendAddress(value);
            riakService.init();
            endpointDelegates.add(riakService);
        });
    }

    @Override
    protected void persistSessionInternal(String sessionId, byte[] bytes) {
        endpointDelegates.forEach(delegate -> delegate.persistSessionInternal(sessionId, bytes));
    }

    @Override
    protected byte[] getSessionInternal(String sessionId) {
        return endpointDelegates.stream().findAny().get().getSessionInternal(sessionId);
    }

    @Override
    protected void deleteSessionInternal(String sessionId) {
        endpointDelegates.forEach(delegate -> delegate.deleteSessionInternal(sessionId));
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

}
