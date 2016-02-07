/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.manager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author lindhrst
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistableSessionUtilsTest {

    @Test
    public void exceptionDuringSerializationIsCaughtAndNullValueReturned() {
        byte[] serializeSession = PersistableSessionUtils.serializeSession(null);
        assertThat(serializeSession, is(nullValue()));
    }

    @Test
    public void exceptionDuringDeserializationIsCaughtAndNullValueReturned() {
        PersistableSession session = PersistableSessionUtils.deserializeSessionInto(null, null);
        assertThat(session, is(nullValue()));
    }
}
