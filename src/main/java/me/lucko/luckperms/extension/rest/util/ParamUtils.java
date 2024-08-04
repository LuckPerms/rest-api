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

package me.lucko.luckperms.extension.rest.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;

/**
 * Parameter reading utilities.
 */
public final class ParamUtils {

    private ParamUtils() {
    }

    public static TemporaryNodeMergeStrategy queryParamAsTemporaryNodeMergeStrategy(ObjectMapper objectMapper, Context ctx) throws JsonProcessingException {
        final String string = ctx.queryParam("temporaryNodeMergeStrategy");

        if (string == null) {
            return TemporaryNodeMergeStrategy.NONE;
        } else {
            return objectMapper.readValue("\"" + string + "\"", TemporaryNodeMergeStrategy.class);
        }
    }

    public static boolean readBoolean(Context ctx, String name, boolean defaultIfMissing) {
        final String string = ctx.queryParam(name);

        if (string == null) {
            return defaultIfMissing;
        } else if (string.equals("true")) {
            return true;
        } else if (string.equals("false")) {
            return false;
        } else {
            throw new IllegalArgumentException("invalid boolean '" + string + "' for query param '" + name + "'");
        }
    }
}
