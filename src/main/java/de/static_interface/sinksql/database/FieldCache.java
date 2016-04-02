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

package de.static_interface.sinksql.database;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * The FieldCache has been added to improve SQL deserialisation speed
 */
public class FieldCache {

    private static Map<String, Annotation> cache = new HashMap<>();

    @Nullable
    public static <T extends Annotation> T getAnnotation(Field f, Class<T> annotation) {
        String name = f.getDeclaringClass().getName() + "." + f.getName();
        if (cache.containsKey(name) && cache.get(name) != null && annotation.isAssignableFrom(cache.get(name).getClass())) {
            return (T) cache.get(name);
        }
        T value = f.getAnnotation(annotation);
        cache.put(name, value);
        return value;
    }
}
