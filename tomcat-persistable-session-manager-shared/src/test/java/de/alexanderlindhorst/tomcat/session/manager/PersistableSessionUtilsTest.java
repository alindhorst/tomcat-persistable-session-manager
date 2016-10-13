/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.tomcat.session.manager;

import de.alexanderlindhorst.tomcat.session.manager.PersistableSessionManager;
import de.alexanderlindhorst.tomcat.session.manager.PersistableSessionUtils;
import de.alexanderlindhorst.tomcat.session.manager.PersistableSession;

import java.io.IOException;
import java.io.ObjectOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

/**
 *
 * @author lindhrst
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistableSessionUtilsTest {

    @Mock
    private PersistableSessionManager manager;
    private PersistableSession session;

    @Before
    public void setup() {
        session = new PersistableSession(manager);
    }

    @Test
    public void nullSessionReturnsNullForSerialization() {
        byte[] serializeSession = PersistableSessionUtils.serializeSession(null);
        assertThat(serializeSession, is(nullValue()));
    }

    @Test
    public void exceptionDuringSerializationIsCaughtAndNullValueReturned() throws IOException {
        PersistableSession spy = spy(session);
        doThrow(new IOException()).when(spy).writeObjectData(any(ObjectOutputStream.class));
        byte[] serializeSession = PersistableSessionUtils.serializeSession(spy);
        assertThat(serializeSession, is(nullValue()));
    }

    @Test
    public void exceptionDuringRealsDeserializationIsCaughtAndNullValueReturned() {
        PersistableSession found = PersistableSessionUtils.deserializeSessionInto(session, new byte[]{1});
        assertThat(found, is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullSessionParameterThrowsIllegalArgumentExceptionForDeserialization() {
        PersistableSessionUtils.deserializeSessionInto(null, new byte[]{1});
    }

    @Test
    public void nullByteArrayParameterReturnsNullForDeserialization() {
        PersistableSessionUtils.deserializeSessionInto(session, null);
    }
}
