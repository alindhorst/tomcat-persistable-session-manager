/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.manager;

import de.alexanderlindhorst.tomcat.session.manager.RiakSessionManager;
import de.alexanderlindhorst.tomcat.session.manager.PersistableSession;
import de.alexanderlindhorst.tomcat.session.manager.PersistableSessionAttribute;

import org.apache.catalina.Context;
import org.apache.catalina.SessionEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static de.alexanderlindhorst.tomcat.session.manager.PersistableSession.SESSION_ATTRIBUTE_SET;
import static org.apache.catalina.Session.SESSION_DESTROYED_EVENT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RiakSessionTest {

    @Mock
    private RiakSessionManager sessionManager;
    @Mock
    private Context context;
    @InjectMocks
    private PersistableSession session;
    @Captor
    private ArgumentCaptor<SessionEvent> captor;

    @Before
    public void setUp() {
        session.setValid(true);
        when(sessionManager.getContext()).thenReturn(context);
        session.addSessionListener(sessionManager);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void setAttributeTriggersSessionEventHandling() {
        String name = "key";
        Object value = "value";
        session.setAttribute(name, value);
        verify(sessionManager).sessionEvent(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().getSession(), is(session));
        assertThat(captor.getValue().getType(), is(SESSION_ATTRIBUTE_SET));
        assertThat(captor.getValue().getData(), is(new PersistableSessionAttribute(name, value)));
    }

    @Test
    public void sessionExpiredTriggersSessionEventHandling() {
        SessionEvent event = new SessionEvent(session, SESSION_DESTROYED_EVENT, null);
        session.expire();
        verify(sessionManager).sessionEvent(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().getSession(), is(session));
        assertThat(captor.getValue().getType(), is(SESSION_DESTROYED_EVENT));
        assertThat(captor.getValue().getData(), is(nullValue()));
    }

}
