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

import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.matcher.NodeMatcher;

import io.javalin.http.Context;

public class SearchRequest {

    public static NodeMatcher<? extends Node> parse(Context ctx) {
        String key = ctx.queryParam("key");
        if (key != null && !key.isEmpty()) {
            return NodeMatcher.key(key);
        }

        String keyStartsWith = ctx.queryParam("keyStartsWith");
        if (keyStartsWith != null && !keyStartsWith.isEmpty()) {
            return NodeMatcher.keyStartsWith(keyStartsWith);
        }

        String metaKey = ctx.queryParam("metaKey");
        if (metaKey != null && !metaKey.isEmpty()) {
            return NodeMatcher.metaKey(metaKey);
        }

        String type = ctx.queryParam("type");
        if (type != null && !type.isEmpty()) {
            NodeType<? extends Node> nodeType = switch (type) {
                case "regex_permission" -> NodeType.REGEX_PERMISSION;
                case "inheritance" -> NodeType.INHERITANCE;
                case "prefix" -> NodeType.PREFIX;
                case "suffix" -> NodeType.SUFFIX;
                case "meta" -> NodeType.META;
                case "weight" -> NodeType.WEIGHT;
                case "display_name" -> NodeType.DISPLAY_NAME;
                default -> throw new IllegalArgumentException("Unknown type: " + type);
            };
            return NodeMatcher.type(nodeType);
        }

        throw new IllegalArgumentException("No query parameter defined");
    }

}
