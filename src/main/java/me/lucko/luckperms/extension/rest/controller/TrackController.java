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

package me.lucko.luckperms.extension.rest.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import me.lucko.luckperms.extension.rest.RestConfig;
import me.lucko.luckperms.extension.rest.model.GroupSearchResult;
import me.lucko.luckperms.extension.rest.model.PermissionCheckRequest;
import me.lucko.luckperms.extension.rest.model.PermissionCheckResult;
import me.lucko.luckperms.extension.rest.model.SearchRequest;
import me.lucko.luckperms.extension.rest.util.ParamUtils;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.track.Track;
import net.luckperms.api.track.TrackManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TrackController {
    private static final boolean CACHE = RestConfig.getBoolean("cache.tracks", true);

    private final TrackManager trackManager;
    private final GroupManager groupManager;
    private final MessagingService messagingService;
    private final ObjectMapper objectMapper;

    public TrackController(TrackManager trackManager, GroupManager groupManager, MessagingService messagingService, ObjectMapper objectMapper) {
        this.trackManager = trackManager;
        this.groupManager = groupManager;
        this.messagingService = messagingService;
        this.objectMapper = objectMapper;
    }

    private CompletableFuture<Track> loadTrackCached(String name) {
        if (CACHE) {
            return CompletableFuture.completedFuture(this.trackManager.getTrack(name));
        } else {
            return this.trackManager.loadTrack(name).thenApply(opt -> opt.orElse(null));
        }
    }

    private CompletableFuture<Set<Track>> loadTracksCached() {
        if (CACHE) {
            return CompletableFuture.completedFuture(this.trackManager.getLoadedTracks());
        } else {
            return this.trackManager.loadAllTracks().thenApply(x -> this.trackManager.getLoadedTracks());
        }
    }

    // POST /track
    public void create(Context ctx) {
        CreateReq body = ctx.bodyAsClass(CreateReq.class);

        if (this.trackManager.isLoaded(body.name)) {
            ctx.status(409).result("Track already exists!");
            return;
        }

        CompletableFuture<Track> future = this.trackManager.createAndLoadTrack(body.name);
        ctx.future(future, result -> ctx.status(201).json(result));
    }

    record CreateReq(@JsonProperty(required = true) String name) { }

    // GET /track
    public void getAll(Context ctx) {
        CompletableFuture<List<String>> future = loadTracksCached()
                .thenApply(tracks -> tracks.stream()
                        .map(Track::getName)
                        .collect(Collectors.toList())
                );
        ctx.future(future);
    }

    // GET /track/{id}
    public void get(Context ctx) {
        String name = ctx.pathParam("id");
        CompletableFuture<Track> future = loadTrackCached(name);
        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404).result("Track doesn't exist");
            } else {
                ctx.json(result);
            }
        });
    }

    // PATCH /track/{id}
    public void update(Context ctx) {
        String name = ctx.pathParam("id");
        UpdateReq body = ctx.bodyAsClass(UpdateReq.class);

        List<Group> groups = new ArrayList<>();
        for (String group : body.groups()) {
            Group g = this.groupManager.getGroup(group);
            if (g == null) {
                ctx.status(404).result("Group " + group + " does not exist");
                return;
            }
            groups.add(g);
        }

        CompletableFuture<Track> future = this.trackManager.loadTrack(name).thenCompose(opt -> {
            if (opt.isPresent()) {
                Track track = opt.get();
                track.clearGroups();
                for (Group group : groups) {
                    track.appendGroup(group);
                }
                return this.trackManager.saveTrack(track).thenApply(v -> {
                    this.messagingService.pushUpdate();
                    return track;
                });
            } else {
                return CompletableFuture.completedFuture(null);
            }
        });

        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404).result("Track doesn't exist");
            } else {
                ctx.result("ok");
            }
        });
    }

    record UpdateReq(@JsonProperty(required = true) List<String> groups) { }

    // DELETE /track/{id}
    public void delete(Context ctx) {
        String name = ctx.pathParam("id");

        CompletableFuture<Boolean> future = this.trackManager.loadTrack(name).thenCompose(opt -> {
            if (opt.isPresent()) {
                return this.trackManager.deleteTrack(opt.get()).thenApply(x -> {
                    this.messagingService.pushUpdate();
                    return true;
                });
            } else {
                return CompletableFuture.completedFuture(false);
            }
        });
        ctx.future(future, result -> {
            if (result == Boolean.FALSE) {
                ctx.status(404).result("Track doesn't exist");
            } else {
                ctx.status(200).result("ok");
            }
        });
    }
}
