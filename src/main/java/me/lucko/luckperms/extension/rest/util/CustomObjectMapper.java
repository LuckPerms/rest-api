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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import me.lucko.luckperms.extension.rest.bind.ActionDeserializer;
import me.lucko.luckperms.extension.rest.bind.ActionSerializer;
import me.lucko.luckperms.extension.rest.bind.ContextSetDeserializer;
import me.lucko.luckperms.extension.rest.bind.ContextSetSerializer;
import me.lucko.luckperms.extension.rest.bind.DemotionResultSerializer;
import me.lucko.luckperms.extension.rest.bind.GroupSerializer;
import me.lucko.luckperms.extension.rest.bind.HealthSerializer;
import me.lucko.luckperms.extension.rest.bind.MetadataSerializer;
import me.lucko.luckperms.extension.rest.bind.NodeDeserializer;
import me.lucko.luckperms.extension.rest.bind.NodeSerializer;
import me.lucko.luckperms.extension.rest.bind.PlayerSaveResultSerializer;
import me.lucko.luckperms.extension.rest.bind.PromotionResultSerializer;
import me.lucko.luckperms.extension.rest.bind.QueryOptionsDeserializer;
import me.lucko.luckperms.extension.rest.bind.TrackSerializer;
import me.lucko.luckperms.extension.rest.bind.UserSerializer;
import me.lucko.luckperms.extension.rest.bind.event.LogBroadcastEventSerializer;
import me.lucko.luckperms.extension.rest.bind.event.PostNetworkSyncEventSerializer;
import me.lucko.luckperms.extension.rest.bind.event.PostSyncEventSerializer;
import me.lucko.luckperms.extension.rest.bind.event.PreNetworkSyncEventSerializer;
import me.lucko.luckperms.extension.rest.bind.event.PreSyncEventSerializer;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.event.log.LogBroadcastEvent;
import net.luckperms.api.event.sync.PostNetworkSyncEvent;
import net.luckperms.api.event.sync.PostSyncEvent;
import net.luckperms.api.event.sync.PreNetworkSyncEvent;
import net.luckperms.api.event.sync.PreSyncEvent;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.platform.Health;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.track.DemotionResult;
import net.luckperms.api.track.PromotionResult;
import net.luckperms.api.track.Track;

public class CustomObjectMapper extends ObjectMapper {

    public CustomObjectMapper() {

        //noinspection deprecation
        this.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        this.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(Action.class, new ActionDeserializer());
        module.addSerializer(Action.class, new ActionSerializer());
        module.addDeserializer(ContextSet.class, new ContextSetDeserializer());
        module.addSerializer(ContextSet.class, new ContextSetSerializer());
        module.addSerializer(DemotionResult.class, new DemotionResultSerializer());
        module.addSerializer(Group.class, new GroupSerializer());
        module.addSerializer(Health.class, new HealthSerializer());
        module.addSerializer(CachedMetaData.class, new MetadataSerializer());
        module.addDeserializer(Node.class, new NodeDeserializer());
        module.addSerializer(Node.class, new NodeSerializer());
        module.addSerializer(PlayerSaveResult.class, new PlayerSaveResultSerializer());
        module.addSerializer(PromotionResult.class, new PromotionResultSerializer());
        module.addDeserializer(QueryOptions.class, new QueryOptionsDeserializer());
        module.addSerializer(Track.class, new TrackSerializer());
        module.addSerializer(User.class, new UserSerializer());

        module.addSerializer(LogBroadcastEvent.class, new LogBroadcastEventSerializer());
        module.addSerializer(PostNetworkSyncEvent.class, new PostNetworkSyncEventSerializer());
        module.addSerializer(PostSyncEvent.class, new PostSyncEventSerializer());
        module.addSerializer(PreNetworkSyncEvent.class, new PreNetworkSyncEventSerializer());
        module.addSerializer(PreSyncEvent.class, new PreSyncEventSerializer());

        this.registerModule(module);
    }

}
