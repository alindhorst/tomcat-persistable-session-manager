package de.alexanderlindhorst.riak.session.manager;

import de.alexanderlindhorst.riak.session.manager.SessionManager;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SessionManagerTest {

    private SessionManager instance;

    @Before
    public void setUp() {
        instance = new SessionManager();
    }

    @Test
    public void rejectedSessionCounterMustWork() {
        assertThat(instance.getRejectedSessions(), is(0));

        instance.rejectSession();
        assertThat(instance.getRejectedSessions(), is(1));

        instance.setRejectedSessions(10);
        assertThat(instance.getRejectedSessions(), is(10));
    }

    @Test
    @Ignore
    public void unloadingMustStoreAllDirtySessions() throws Exception {
        fail("Not yet supported");
    }

}
