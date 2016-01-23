package de.alexanderlindhorst.riak.session;

import java.lang.reflect.Field;

/**
 * @author lindhrst (original author)
 */
public final class TestUtils {

    private TestUtils() {
        //Utility class
    }

    public static Object getFieldValueFromObject(Object object, String fieldName) throws NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }
}
