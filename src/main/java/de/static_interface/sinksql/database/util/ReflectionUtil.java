/*
 * Copyright (c) 2013 - 2016 Trojaner <trojaner25@gmail.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.static_interface.sinksql.database.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReflectionUtil {

    public final static Map<Class<?>, Class<?>> primitiveMap = new HashMap<>();
    public final static Map<Class<?>, Method> primiviteMethods = new HashMap<>();

    static {
        primitiveMap.put(boolean.class, Boolean.class);
        primitiveMap.put(byte.class, Byte.class);
        primitiveMap.put(short.class, Short.class);
        primitiveMap.put(char.class, Character.class);
        primitiveMap.put(int.class, Integer.class);
        primitiveMap.put(long.class, Long.class);
        primitiveMap.put(float.class, Float.class);
        primitiveMap.put(double.class, Double.class);
    }

    static {
        Class<?> numberClass = Number.class;
        try {
            primiviteMethods.put(byte.class, numberClass.getMethod("byteValue"));
            primiviteMethods.put(short.class, numberClass.getMethod("shortValue"));
            primiviteMethods.put(int.class, numberClass.getMethod("intValue"));
            primiviteMethods.put(long.class, numberClass.getMethod("longValue"));
            primiviteMethods.put(float.class, numberClass.getMethod("floatValue"));
            primiviteMethods.put(double.class, numberClass.getMethod("doubleValue"));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static List<Field> getAllFields(Class<?> type) {
        List<Field> fieldz = new ArrayList<>();
        fieldz.addAll(Arrays.asList(type.getDeclaredFields()));

        while (type.getSuperclass() != null) {
            type = type.getSuperclass();
            fieldz.addAll(Arrays.asList(type.getDeclaredFields()));
        }

        return fieldz;
    }

    public static <T> T invoke(Object object, String methodName, Class<T> returnClass, Object... args) {
        try {
            Method method = object.getClass().getMethod(methodName);
            method.setAccessible(true);
            return (T) method.invoke(object, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T invokeStatic(Class<?> clazz, String methodName, Class<T> returnClass, Object... args) {
        try {
            Method method = clazz.getMethod(methodName);
            method.setAccessible(true);
            return (T) method.invoke(null, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isPrimitiveClass(Class<?> clazz) {
        return clazz.isPrimitive();
    }

    public static boolean isNumber(Class<?> type) {
        return Number.class.isAssignableFrom(type)
               || byte.class.isAssignableFrom(type)
               || short.class.isAssignableFrom(type)
               || int.class.isAssignableFrom(type)
               || float.class.isAssignableFrom(type)
               || long.class.isAssignableFrom(type)
               || double.class.isAssignableFrom(type);
    }

    public static Object primitiveToWrapper(Object c) {
        if (isWrapperClass(c.getClass())) {
            return c;
        }
        Class<?> wrapperClass = primitiveMap.get(c.getClass());

        if (char.class.isAssignableFrom(c.getClass())) {
            return Character.valueOf((char) c);
        }

        try {
            Method m = wrapperClass.getMethod("valueOf", String.class);
            return m.invoke(null, c.toString());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isWrapperClass(Class<?> clazz) {
        return primitiveMap.values().contains(clazz);
    }

    public static Method getDeclaredMethod(Class<?> type, String name, Class<?>... params) throws NoSuchMethodException {
        Method m = null;
        try {
            m = type.getDeclaredMethod(name, params);
        } catch (NoSuchMethodException ignored) {

        }

        if (m != null) {
            return m;
        }
        while (type.getSuperclass() != null) {
            type = type.getSuperclass();
            try {
                m = type.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException ignored) {

            }

            if (m != null) {
                return m;
            }
        }

        throw new NoSuchMethodException();
    }

    public static Object getDeclaredField(Object object, String field) {
        try {
            Class<?> clazz = object.getClass();
            Field objectField = clazz.getDeclaredField(field);
            objectField.setAccessible(true);
            Object result = objectField.get(object);
            objectField.setAccessible(false);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object wrapperToPrimitive(Object value) {
        if (isPrimitiveClass(value.getClass())) {
            return value;
        }

        if (primiviteMethods.keySet().contains(value.getClass())) {
            Method m = primiviteMethods.get(value.getClass());
            try {
                return m.invoke(value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }

        if (value instanceof Character) {
            return ((Character) value).charValue();
        }

        return value;
    }
}
