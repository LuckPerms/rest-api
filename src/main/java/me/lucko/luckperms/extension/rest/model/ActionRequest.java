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

package me.lucko.luckperms.extension.rest.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import net.luckperms.api.actionlog.filter.ActionFilter;

import java.util.UUID;

public class ActionRequest {

    private static UUID parseUuid(ObjectMapper objectMapper, String s) throws JsonProcessingException {
        String uuidString = "\"" + s + "\"";
        return objectMapper.readValue(uuidString, UUID.class);
    }

    public static ActionFilter parseFilter(ObjectMapper objectMapper, Context ctx) throws JsonProcessingException {
        String source = ctx.queryParam("source");
        if (source != null && !source.isEmpty()) {
            return ActionFilter.source(parseUuid(objectMapper, source));
        }

        String user = ctx.queryParam("user");
        if (user != null && !user.isEmpty()) {
            return ActionFilter.user(parseUuid(objectMapper, user));
        }

        String group = ctx.queryParam("group");
        if (group != null && !group.isEmpty()) {
            return ActionFilter.group(group);
        }

        String track = ctx.queryParam("track");
        if (track != null && !track.isEmpty()) {
            return ActionFilter.track(track);
        }

        String search = ctx.queryParam("search");
        if (search != null && !search.isEmpty()) {
            return ActionFilter.search(search);
        }

        return ActionFilter.any();
    }

}
