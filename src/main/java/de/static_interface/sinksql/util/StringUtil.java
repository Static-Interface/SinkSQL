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

package de.static_interface.sinksql.util;

import javax.annotation.Nullable;

public class StringUtil {
    public static boolean isEmptyOrNull(@Nullable String s) {
        return s == null || s.trim().length() == 0 || s.trim().isEmpty();
    }
    public static String formatArrayToString(Object[] input, @Nullable String character) {
        return formatArrayToString(input, character, 0, input.length);
    }

    public static String formatArrayToString(Object[] input, @Nullable String character, int startIndex, int endIndex) {
        if (input == null || input.length == 0) {
            return "";
        }

        if (startIndex < 0) {
            throw new IllegalArgumentException("startIndex can't be less than 0 !");
        }
        if (endIndex <= startIndex) {
            throw new IllegalArgumentException("endIndex can't be less or equal startIndex!");
        }

        if (character == null) {
            character = " ";
        }

        String tmp = "";
        for (int i = startIndex; i < endIndex; i++) {
            if (input[i] == null) {
                continue;
            }

            if (tmp.equals("")) {
                tmp = input[i].toString();
                continue;
            }
            tmp += character + input[i].toString();
        }

        return tmp;
    }
}
