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

package me.lucko.luckperms.extension.rest.bind;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;

import java.io.IOException;

public class NodeDeserializer extends JsonDeserializer<Node> {

    @Override
    public Node deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        return p.readValueAs(Model.class).to();
    }

    record Model(@JsonProperty(required = true) String key, Boolean value, ContextSet context, Long expiry) {
        Node to() {
            NodeBuilder<?, ?> builder = Node.builder(this.key);
            if (this.value != null) {
                builder.value(this.value);
            }
            if (this.context != null) {
                builder.context(this.context);
            }
            if (this.expiry != null) {
                builder.expiry(this.expiry);
            }

            return builder.build();
        }
    }

}
