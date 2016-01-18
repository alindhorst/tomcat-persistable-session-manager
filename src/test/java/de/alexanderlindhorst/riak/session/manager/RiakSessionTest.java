/*
 *  LICENSE INFORMATION:
 */
package de.alexanderlindhorst.riak.session.manager;

import org.apache.catalina.SessionEvent;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class RiakSessionTest {

    @Mock
    private RiakSessionManager sessionManager;
    @InjectMocks
    private RiakSession session;

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void setAttributeTriggersSessionEventTriggersPersisting() {
        String name = "";
        Object value = "";
        session.setAttribute(name, value);
        verify(sessionManager).sessionEvent(new SessionEvent(session, name, value));
    }

}
