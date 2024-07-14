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
import me.lucko.luckperms.extension.rest.model.PermissionCheckRequest;
import me.lucko.luckperms.extension.rest.model.PermissionCheckResult;
import me.lucko.luckperms.extension.rest.model.SearchRequest;
import me.lucko.luckperms.extension.rest.model.TrackRequest;
import me.lucko.luckperms.extension.rest.model.UserLookupResult;
import me.lucko.luckperms.extension.rest.model.UserSearchResult;
import me.lucko.luckperms.extension.rest.util.ParamUtils;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.track.DemotionResult;
import net.luckperms.api.track.PromotionResult;
import net.luckperms.api.track.Track;
import net.luckperms.api.track.TrackManager;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserController implements PermissionHolderController {
    private static final boolean CACHE = RestConfig.getBoolean("cache.users", true);

    private final UserManager userManager;
    private final TrackManager trackManager;
    private final MessagingService messagingService;
    private final ObjectMapper objectMapper;

    public UserController(UserManager userManager, TrackManager trackManager, MessagingService messagingService, ObjectMapper objectMapper) {
        this.userManager = userManager;
        this.trackManager = trackManager;
        this.messagingService = messagingService;
        this.objectMapper = objectMapper;
    }

    private UUID parseUuid(String s) throws JsonProcessingException {
        String uuidString = "\"" + s + "\"";
        return this.objectMapper.readValue(uuidString, UUID.class);
    }

    private UUID pathParamAsUuid(Context ctx) throws JsonProcessingException {
        return parseUuid(ctx.pathParam("id"));
    }

    private CompletableFuture<User> loadUserCached(UUID uniqueId) {
        if (CACHE) {
            User user = this.userManager.getUser(uniqueId);
            if (user != null) {
                return CompletableFuture.completedFuture(user);
            }
        }
        return this.userManager.loadUser(uniqueId);
    }

    // POST /user
    @Override
    public void create(Context ctx) {
        CreateReq body = ctx.bodyAsClass(CreateReq.class);

        CompletableFuture<PlayerSaveResult> future = this.userManager.savePlayerData(body.uniqueId, body.username);
        ctx.future(future, result -> {
            if (((PlayerSaveResult) result).includes(PlayerSaveResult.Outcome.CLEAN_INSERT)) {
                ctx.status(201);
            }
            ctx.json(result);
        });
    }

    record CreateReq(@JsonProperty(required = true) UUID uniqueId, @JsonProperty(required = true) String username) { }

    // GET /user
    @Override
    public void getAll(Context ctx) {
        CompletableFuture<Set<UUID>> future = this.userManager.getUniqueUsers();
        ctx.future(future);
    }

    // GET /user/search
    @Override
    public void search(Context ctx) throws Exception {
        NodeMatcher<? extends Node> matcher = SearchRequest.parse(ctx);
        CompletableFuture<List<UserSearchResult>> future = this.userManager.<Node>searchAll(matcher)
                .thenApply(map -> map.entrySet().stream()
                        .map(e -> new UserSearchResult(e.getKey(), e.getValue()))
                        .toList()
                );
        ctx.future(future);
    }

    // GET /user/lookup
    public void lookup(Context ctx) throws Exception {
        String usernameParam = ctx.queryParam("username");
        String uniqueIdParam = ctx.queryParam("uniqueId");

        CompletableFuture<UserLookupResult> future;
        if (usernameParam != null && !usernameParam.isEmpty()) {
            future = this.userManager.lookupUniqueId(usernameParam)
                    .thenApply(uniqueId -> uniqueId == null
                            ? null
                            : new UserLookupResult(usernameParam, uniqueId));

        } else if (uniqueIdParam != null && !uniqueIdParam.isEmpty()) {
            UUID parsedUniqueId = parseUuid(uniqueIdParam);
            future = this.userManager.lookupUsername(parsedUniqueId)
                    .thenApply(username -> username == null || username.isEmpty()
                            ? null
                            : new UserLookupResult(username, parsedUniqueId));

        } else {
            throw new IllegalArgumentException("Must specify username or unique id");
        }

        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404);
            } else {
                ctx.json(result);
            }
        });
    }

    // GET /user/{id}
    @Override
    public void get(Context ctx) throws JsonProcessingException {
        UUID uniqueId = pathParamAsUuid(ctx);
        ctx.future(loadUserCached(uniqueId), result -> {
            if (result == null) {
                ctx.status(404);
            } else {
                ctx.json(result);
            }
        });
    }

    // PATCH /user/{id}
    @Override
    public void update(Context ctx) throws JsonProcessingException {
        UUID uniqueId = pathParamAsUuid(ctx);
        UpdateReq body = ctx.bodyAsClass(UpdateReq.class);
        ctx.future(this.userManager.savePlayerData(uniqueId, body.username), result -> ctx.result("ok"));
    }

    record UpdateReq(@JsonProperty(required = true) String username) { }

    // DELETE /user/{id}
    @Override
    public void delete(Context ctx) throws JsonProcessingException {
        UUID uniqueId = pathParamAsUuid(ctx);
        boolean playerDataOnly = ctx.queryParamAsClass("playerDataOnly", Boolean.class).getOrDefault(false);

        CompletableFuture<Void> future;
        if (playerDataOnly) {
            future = this.userManager.deletePlayerData(uniqueId);
        } else {
            future = this.userManager.loadUser(uniqueId).thenCompose(user -> {
                user.data().clear();
                return this.userManager.saveUser(user).thenCompose(ignored -> {
                    this.messagingService.pushUserUpdate(user);
                    return this.userManager.deletePlayerData(uniqueId);
                });
            });
        }

        ctx.future(future, result -> ctx.result("ok"));
    }

    // GET /user/{id}/nodes
    @Override
    public void nodesGet(Context ctx) throws JsonProcessingException {
        UUID uniqueId = pathParamAsUuid(ctx);
        CompletableFuture<Collection<Node>> future = loadUserCached(uniqueId).thenApply(PermissionHolder::getNodes);
        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404);
            } else {
                ctx.json(result);
            }
        });
    }

    // PATCH /user/{id}/nodes
    @Override
    public void nodesAddMultiple(Context ctx) throws JsonProcessingException {
        UUID uniqueId = pathParamAsUuid(ctx);
        List<Node> nodes = this.objectMapper.readValue(ctx.body(), new TypeReference<>(){});
        TemporaryNodeMergeStrategy mergeStrategy = ParamUtils.queryParamAsTemporaryNodeMergeStrategy(this.objectMapper, ctx);

        CompletableFuture<Collection<Node>> future = this.userManager.loadUser(uniqueId).thenCompose(user -> {
            for (Node node : nodes) {
                user.data().add(node, mergeStrategy);
            }
            return this.userManager.saveUser(user).thenApply(v -> {
                this.messagingService.pushUserUpdate(user);
                return user.getNodes();
            });
        });
        ctx.future(future);
    }


    // DELETE /user/{id}/nodes
    @Override
    public void nodesDeleteAll(Context ctx) throws JsonProcessingException {
        UUID uniqueId = pathParamAsUuid(ctx);
        List<Node> nodes = ctx.body().isEmpty()
                ? null
                : this.objectMapper.readValue(ctx.body(), new TypeReference<>(){});

        CompletableFuture<?> future = this.userManager.loadUser(uniqueId).thenCompose(user -> {
            if (nodes == null) {
                user.data().clear();
            } else {
                for (Node node : nodes) {
                    user.data().remove(node);
                }
            }
            return this.userManager.saveUser(user).thenApply(v -> {
                this.messagingService.pushUserUpdate(user);
                return user.getNodes();
            });
        });
        ctx.future(future, result -> ctx.result("ok"));
    }

    // POST /user/{id}/nodes
    @Override
    public void nodesAddSingle(Context ctx) throws JsonProcessingException {
        UUID uniqueId = pathParamAsUuid(ctx);
        Node node = ctx.bodyAsClass(Node.class);
        TemporaryNodeMergeStrategy mergeStrategy = ParamUtils.queryParamAsTemporaryNodeMergeStrategy(this.objectMapper, ctx);

        CompletableFuture<Collection<Node>> future = this.userManager.loadUser(uniqueId).thenCompose(user -> {
            user.data().add(node, mergeStrategy);
            return this.userManager.saveUser(user).thenApply(v -> {
                this.messagingService.pushUserUpdate(user);
                return user.getNodes();
            });
        });
        ctx.future(future);
    }

    // PUT /user/{id}/nodes
    @Override
    public void nodesSet(Context ctx) throws JsonProcessingException {
        UUID uniqueId = pathParamAsUuid(ctx);
        List<Node> nodes = this.objectMapper.readValue(ctx.body(), new TypeReference<>(){});

        CompletableFuture<Collection<Node>> future = this.userManager.loadUser(uniqueId).thenCompose(user -> {
            user.data().clear();
            for (Node node : nodes) {
                user.data().add(node);
            }
            return this.userManager.saveUser(user).thenApply(v -> {
                this.messagingService.pushUserUpdate(user);
                return user.getNodes();
            });
        });
        ctx.future(future);
    }

    // GET /user/{id}/meta
    @Override
    public void metaGet(Context ctx) throws JsonProcessingException {
        UUID uniqueId = pathParamAsUuid(ctx);
        CompletableFuture<CachedMetaData> future = loadUserCached(uniqueId)
                .thenApply(user -> user.getCachedData().getMetaData());
        ctx.future(future);
    }

    // GET /user/{id}/permission-check
    @Override
    public void permissionCheck(Context ctx) throws JsonProcessingException {
        UUID uniqueId = pathParamAsUuid(ctx);
        String permission = ctx.queryParam("permission");
        if (permission == null || permission.isEmpty()) {
            throw new IllegalArgumentException("Missing permission");
        }

        CompletableFuture<PermissionCheckResult> future = loadUserCached(uniqueId)
                .thenApply(user -> user.getCachedData().getPermissionData().queryPermission(permission))
                .thenApply(PermissionCheckResult::from);

        ctx.future(future);
    }

    // POST /user/{id}/permission-check
    @Override
    public void permissionCheckCustom(Context ctx) throws JsonProcessingException {
        UUID uniqueId = pathParamAsUuid(ctx);
        PermissionCheckRequest req = ctx.bodyAsClass(PermissionCheckRequest.class);
        if (req.permission() == null || req.permission().isEmpty()) {
            throw new IllegalArgumentException("Missing permission");
        }

        CompletableFuture<PermissionCheckResult> future = this.userManager.loadUser(uniqueId)
                .thenApply(user -> {
                    QueryOptions options = req.queryOptions();
                    if (options == null) {
                        options = user.getQueryOptions();
                    }
                    return user.getCachedData().getPermissionData(options).queryPermission(req.permission());
                })
                .thenApply(PermissionCheckResult::from);

        ctx.future(future);
    }

    // POST /user/{id}/promote
    @Override
    public void promote(Context ctx) throws Exception {
        UUID uniqueId = pathParamAsUuid(ctx);
        TrackRequest req = ctx.bodyAsClass(TrackRequest.class);
        if (req.track() == null || req.track().isEmpty()) {
            throw new IllegalArgumentException("Missing track");
        }

        ContextSet context = req.context() == null ? ImmutableContextSet.empty() : req.context();

        CompletableFuture<PromotionResult> future = this.trackManager.loadTrack(req.track()).thenCompose(opt -> {
            if (opt.isPresent()) {
                Track track = opt.get();
                return this.userManager.loadUser(uniqueId).thenCompose(user -> {
                    PromotionResult result = track.promote(user, context);
                    return this.userManager.saveUser(user).thenApply(x -> result);
                });
            } else {
                return CompletableFuture.completedFuture(null);
            }
        });

        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404);
            } else {
                ctx.json(result);
            }
        });
    }

    // POST /user/{id}/demote
    @Override
    public void demote(Context ctx) throws Exception {
        UUID uniqueId = pathParamAsUuid(ctx);
        TrackRequest req = ctx.bodyAsClass(TrackRequest.class);
        if (req.track() == null || req.track().isEmpty()) {
            throw new IllegalArgumentException("Missing track");
        }

        ContextSet context = req.context() == null ? ImmutableContextSet.empty() : req.context();

        CompletableFuture<DemotionResult> future = this.trackManager.loadTrack(req.track()).thenCompose(opt -> {
            if (opt.isPresent()) {
                Track track = opt.get();
                return this.userManager.loadUser(uniqueId).thenCompose(user -> {
                    DemotionResult result = track.demote(user, context);
                    return this.userManager.saveUser(user).thenApply(x -> result);
                });
            } else {
                return CompletableFuture.completedFuture(null);
            }
        });

        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404);
            } else {
                ctx.json(result);
            }
        });
    }
}
