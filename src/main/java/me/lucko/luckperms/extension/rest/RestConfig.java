/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.extension.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class RestConfig {

    public static String getString(String path, String defaultValue) {
        String sysProperty = "luckperms.rest." + path;
        String envVariable = sysProperty.toUpperCase(Locale.ROOT).replace('.', '_');

        String value = System.getProperty(sysProperty, System.getenv(envVariable));
        if (value != null) {
            return value;
        } else {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String path, boolean defaultValue) {
        String value = getString(path, null);
        if (value != null) {
            return Boolean.parseBoolean(value);
        } else {
            return defaultValue;
        }
    }


    public static int getInteger(String path, int defaultValue) {
        String value = getString(path, null);
        if (value != null) {
            return Integer.parseInt(value);
        } else {
            return defaultValue;
        }
    }

    public static List<String> getStringList(String path, List<String> defaultValue) {
        String value = getString(path, null);
        if (value != null) {
            return Arrays.asList(value.split(","));
        } else {
            return defaultValue;
        }
    }

}
