/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;

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

    public static void setFieldValueForObject(Object object, String fieldName, Object value) throws
            IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }

    public static Object invokeMethod(Object object, String methodName, Parameter... parameters) throws
            NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method method = findMethod(object.getClass(), methodName, parameters);
        method.setAccessible(true);
        return method.invoke(object, getParameterValuesFromParameters(parameters));
    }

    private static Method findMethod(Class<?> clazz, String methodName, Parameter... parameters) throws
            SecurityException, NoSuchMethodException {
        try {
            return clazz.getDeclaredMethod(methodName, getParameterClassesFromParameters(parameters));
        } catch (NoSuchMethodException ex) {
            if (clazz.getSuperclass() != null) {
                return findMethod(clazz.getSuperclass(), methodName, parameters);
            } else {
                throw new NoSuchMethodException();
            }
        }
    }

    private static Class<?>[] getParameterClassesFromParameters(Parameter... parameters) {
        ArrayList<Parameter> params = newArrayList(parameters);
        ArrayList<Class<?>> classes = newArrayListWithCapacity(params.size());
        params.forEach((Parameter parameter) -> classes.add(parameter.parameterClass));
        return classes.toArray(new Class<?>[classes.size()]);
    }

    private static Object[] getParameterValuesFromParameters(Parameter... parameters) {
        ArrayList<Parameter> params = newArrayList(parameters);
        ArrayList<Object> values = newArrayListWithCapacity(params.size());
        params.forEach((Parameter parameter) -> values.add(parameter.value));
        return values.toArray(new Object[values.size()]);
    }

    public static class Parameter {

        private final Class<?> parameterClass;
        private final Object value;

        public Parameter(
                Class<?> parameterClass, Object value) {
            this.parameterClass = parameterClass;
            this.value = value;
        }

    }
}
