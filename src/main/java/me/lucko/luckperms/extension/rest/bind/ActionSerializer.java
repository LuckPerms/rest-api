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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.luckperms.api.actionlog.Action;

import java.io.IOException;
import java.util.UUID;

public class ActionSerializer extends JsonSerializer<Action> {

    @Override
    public void serialize(Action value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writePOJO(Model.from(value));
    }


    record Model(long timestamp, SourceModel source, TargetModel target, String description) {
        static Model from(Action action) {
            return new Model(
                    action.getTimestamp().getEpochSecond(),
                    SourceModel.from(action.getSource()),
                    TargetModel.from(action.getTarget()),
                    action.getDescription()
            );
        }
    }

    record SourceModel(UUID uniqueId, String name) {
        static SourceModel from(Action.Source source) {
            return new SourceModel(source.getUniqueId(), source.getName());
        }
    }

    record TargetModel(UUID uniqueId, String name, Action.Target.Type type) {
        static TargetModel from(Action.Target action) {
            return new TargetModel(action.getUniqueId().orElse(null), action.getName(), action.getType());
        }
    }
}
