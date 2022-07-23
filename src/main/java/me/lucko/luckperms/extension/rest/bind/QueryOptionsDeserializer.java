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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;

import java.io.IOException;
import java.util.Set;

public class QueryOptionsDeserializer extends JsonDeserializer<QueryOptions> {

    @Override
    public QueryOptions deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return p.readValueAs(Model.class).to();
    }

    record Model(QueryMode mode, Set<Flag> flags, ContextSet contexts) {
        QueryOptions to() {
            if (this.mode == null && this.flags == null && this.contexts == null) {
                return null;
            }

            QueryMode mode = this.mode == null
                    ? QueryMode.CONTEXTUAL
                    : this.mode;

            Set<Flag> flags = this.flags == null
                    ? QueryOptions.defaultContextualOptions().flags()
                    : this.flags;

            ContextSet contexts = this.flags == null
                    ? ImmutableContextSet.empty()
                    : this.contexts;

            return QueryOptions.builder(mode)
                    .flags(flags)
                    .context(contexts)
                    .build();
        }
    }

}
