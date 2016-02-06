/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.access;

/**
 * @author lindhrst (original author)
 */
@SuppressWarnings("serial")
public class RiakAccessException extends RuntimeException {

    public RiakAccessException(String message, Throwable cause) {
        super(message, cause);
    }

}
