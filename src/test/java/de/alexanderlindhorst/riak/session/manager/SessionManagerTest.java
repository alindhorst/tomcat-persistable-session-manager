package de.alexanderlindhorst.riak.session.manager;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.alexanderlindhorst.riak.session.access.RiakService;

import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class SessionManagerTest {

    @Mock
    private RiakService riakService;
    @InjectMocks
    private RiakSessionManager instance;

    @Test
    @Ignore
    public void createSessionBuildsANewSession() {
        fail("");
    }

}
