/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static de.alexanderlindhorst.riak.session.manager.BackendServiceBase.LOGGER;

/**
 * @author lindhrst (original author)
 */
public class PersistableSessionUtils {

    public static byte[] serializeSession(PersistableSession session) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(out);
            session.writeObjectData(stream);
            stream.flush();
            byte[] bytes = out.toByteArray();
            out.close();
            return bytes;
        } catch (IOException iOException) {
            LOGGER.error("Couldn't serialize session, will return null value", iOException);
        }
        return null;
    }

    public static PersistableSession deserializeSessionInto(PersistableSession emptyShell, byte[] bytes) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream stream = new ObjectInputStream(in);
            emptyShell.readObjectData(stream);
            stream.close();
            in.close();
            return emptyShell;
        } catch (IOException | ClassNotFoundException ex) {
            LOGGER.error("Couldn't deserialize session, will return null value", ex);
        }
        return null;
    }
}
