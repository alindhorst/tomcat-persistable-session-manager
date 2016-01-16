package de.alexanderlindhorst.riak.session.manager;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.alexanderlindhorst.riak.session.access.RiakService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SessionManagerTest {

    @Mock
    private RiakService riakService;
    @InjectMocks
    private RiakSessionManager instance;

    @Test
    public void getNewSessionCreatesManagerAwareRiakSession() {
        StandardSession newSession = instance.getNewSession();
        assertThat(newSession.getManager(), is((Manager) instance));
    }

    @Test
    @Ignore
    public void createEmptySessionCreatesSessionWithId() {
        RiakSession emptySession = (RiakSession) instance.createEmptySession();
        assertThat(emptySession.getId(), is(notNull(String.class)));
    }

    @Test
    @Ignore
    public void createEmptySessionPersistsRiakSession() {
        RiakSession emptySession = (RiakSession) instance.createEmptySession();
        verify(riakService).persistSession(emptySession);
    }

}
