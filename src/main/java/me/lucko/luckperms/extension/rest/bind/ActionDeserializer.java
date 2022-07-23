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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import net.luckperms.api.actionlog.Action;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

public class ActionDeserializer extends JsonDeserializer<Action> {

    @Override
    public Action deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return p.readValueAs(Model.class).to();
    }

    record Model(
            Long timestamp,
            @JsonProperty(required = true) SourceModel source,
            @JsonProperty(required = true) TargetModel target,
            @JsonProperty(required = true) String description
    ) {
        Action to() {
            long timestamp;
            if (this.timestamp == null || this.timestamp == 0) {
                timestamp = System.currentTimeMillis() / 1000L;
            } else {
                timestamp = this.timestamp;
            }

            return Action.builder()
                    .timestamp(Instant.ofEpochSecond(timestamp))
                    .source(this.source.uniqueId())
                    .sourceName(this.source.name())
                    .target(this.target.uniqueId())
                    .targetName(this.target.name())
                    .targetType(this.target.type())
                    .description(this.description)
                    .build();
        }
    }

    record SourceModel(
            @JsonProperty(required = true) UUID uniqueId,
            @JsonProperty(required = true) String name
    ) { }

    record TargetModel(
            UUID uniqueId,
            @JsonProperty(required = true) String name,
            @JsonProperty(required = true) Action.Target.Type type

    ) { }

}
