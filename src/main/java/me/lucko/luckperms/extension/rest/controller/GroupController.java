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

import me.lucko.luckperms.extension.rest.model.GroupSearchResult;
import me.lucko.luckperms.extension.rest.model.PermissionCheckRequest;
import me.lucko.luckperms.extension.rest.model.PermissionCheckResult;
import me.lucko.luckperms.extension.rest.model.SearchRequest;

import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;
import net.luckperms.api.query.QueryOptions;

import io.javalin.http.Context;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GroupController implements PermissionHolderController {

    private final GroupManager groupManager;
    private final MessagingService messagingService;
    private final ObjectMapper objectMapper;

    public GroupController(GroupManager groupManager, MessagingService messagingService, ObjectMapper objectMapper) {
        this.groupManager = groupManager;
        this.messagingService = messagingService;
        this.objectMapper = objectMapper;
    }

    // POST /group
    @Override
    public void create(Context ctx) {
        CreateReq body = ctx.bodyAsClass(CreateReq.class);

        if (this.groupManager.isLoaded(body.name)) {
            ctx.status(409).result("Group already exists!");
            return;
        }

        CompletableFuture<Group> future = this.groupManager.createAndLoadGroup(body.name);
        ctx.future(future, result -> ctx.status(201).json(result));
    }

    record CreateReq(@JsonProperty(required = true) String name) { }

    // GET /group
    @Override
    public void getAll(Context ctx) {
        CompletableFuture<List<String>> future = this.groupManager.loadAllGroups()
                .thenApply(x -> this.groupManager.getLoadedGroups().stream()
                        .map(Group::getName)
                        .collect(Collectors.toList()
                        )
                );
        ctx.future(future);
    }

    // GET /group/search
    @Override
    public void search(Context ctx) throws Exception {
        NodeMatcher<? extends Node> matcher = SearchRequest.parse(ctx);
        CompletableFuture<List<GroupSearchResult>> future = this.groupManager.<Node>searchAll(matcher)
                .thenApply(map -> map.entrySet().stream()
                        .map(e -> new GroupSearchResult(e.getKey(), e.getValue()))
                        .toList()
                );
        ctx.future(future);
    }

    // GET /group/{id}
    @Override
    public void get(Context ctx) {
        String name = ctx.pathParam("id");
        CompletableFuture<Group> future = this.groupManager.loadGroup(name).thenApply(opt -> opt.orElse(null));
        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404).result("Group doesn't exist");
            } else {
                ctx.json(result);
            }
        });
    }

    // PATCH /group/{id}
    @Override
    public void update(Context ctx) {
        throw new UnsupportedOperationException("Groups do not have any modifiable fields");
    }

    // DELETE /group/{id}
    @Override
    public void delete(Context ctx) {
        String name = ctx.pathParam("id");

        CompletableFuture<Boolean> future = this.groupManager.loadGroup(name).thenCompose(opt -> {
            if (opt.isPresent()) {
                return this.groupManager.deleteGroup(opt.get()).thenApply(x -> {
                    this.messagingService.pushUpdate();
                    return true;
                });
            } else {
                return CompletableFuture.completedFuture(false);
            }
        });
        ctx.future(future, result -> {
            if (result == Boolean.FALSE) {
                ctx.status(404).result("Group doesn't exist");
            } else {
                ctx.status(200).result("ok");
            }
        });
    }

    // GET /group/{id}/nodes
    @Override
    public void nodesGet(Context ctx) {
        String name = ctx.pathParam("id");
        CompletableFuture<Collection<Node>> future = this.groupManager.loadGroup(name)
                .thenApply(opt -> opt.map(PermissionHolder::getNodes).orElse(null));
        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404).result("Group doesn't exist");
            } else {
                ctx.json(result);
            }
        });
    }

    // PATCH /group/{id}/nodes
    @Override
    public void nodesAddMultiple(Context ctx) throws JsonProcessingException {
        String name = ctx.pathParam("id");
        List<Node> nodes = this.objectMapper.readValue(ctx.body(), new TypeReference<>(){});

        CompletableFuture<Collection<Node>> future = this.groupManager.loadGroup(name).thenCompose(opt -> {
            if (opt.isPresent()) {
                Group group = opt.get();
                for (Node node : nodes) {
                    group.data().add(node);
                }
                return this.groupManager.saveGroup(group).thenApply(v -> {
                    this.messagingService.pushUpdate();
                    return group.getNodes();
                });
            } else {
                return CompletableFuture.completedFuture(null);
            }
        });

        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404).result("Group doesn't exist");
            } else {
                ctx.json(result);
            }
        });
    }


    // DELETE /group/{id}/nodes
    @Override
    public void nodesDeleteAll(Context ctx) throws JsonProcessingException {
        String name = ctx.pathParam("id");
        List<Node> nodes = ctx.body().isEmpty()
                ? null
                : this.objectMapper.readValue(ctx.body(), new TypeReference<>(){});

        CompletableFuture<Boolean> future = this.groupManager.loadGroup(name).thenCompose(opt -> {
            if (opt.isPresent()) {
                Group group = opt.get();
                if (nodes == null) {
                    group.data().clear();
                } else {
                    for (Node node : nodes) {
                        group.data().remove(node);
                    }
                }
                return this.groupManager.saveGroup(group).thenApply(x -> {
                    this.messagingService.pushUpdate();
                    return true;
                });
            } else {
                return CompletableFuture.completedFuture(false);
            }
        });
        ctx.future(future, result -> {
            if (result == Boolean.FALSE) {
                ctx.status(404).result("Group doesn't exist");
            } else {
                ctx.status(200).result("ok");
            }
        });
    }

    // POST /group/{id}/nodes
    @Override
    public void nodesAddSingle(Context ctx) {
        String name = ctx.pathParam("id");
        Node node = ctx.bodyAsClass(Node.class);

        CompletableFuture<Collection<Node>> future = this.groupManager.loadGroup(name).thenCompose(opt -> {
            if (opt.isPresent()) {
                Group group = opt.get();
                group.data().add(node);
                return this.groupManager.saveGroup(group).thenApply(v -> {
                    this.messagingService.pushUpdate();
                    return group.getNodes();
                });
            } else {
                return CompletableFuture.completedFuture(null);
            }
        });

        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404).result("Group doesn't exist");
            } else {
                ctx.json(result);
            }
        });
    }

    // PUT /group/{id}/nodes
    @Override
    public void nodesSet(Context ctx) throws JsonProcessingException {
        String name = ctx.pathParam("id");
        List<Node> nodes = this.objectMapper.readValue(ctx.body(), new TypeReference<>(){});

        CompletableFuture<Collection<Node>> future = this.groupManager.loadGroup(name).thenCompose(opt -> {
            if (opt.isPresent()) {
                Group group = opt.get();
                group.data().clear();
                for (Node node : nodes) {
                    group.data().add(node);
                }
                return this.groupManager.saveGroup(group).thenApply(v -> {
                    this.messagingService.pushUpdate();
                    return group.getNodes();
                });
            } else {
                return CompletableFuture.completedFuture(null);
            }
        });

        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404).result("Group doesn't exist");
            } else {
                ctx.json(result);
            }
        });
    }

    // GET /group/{id}/meta
    @Override
    public void metaGet(Context ctx) {
        String name = ctx.pathParam("id");
        CompletableFuture<CachedMetaData> future = this.groupManager.loadGroup(name)
                .thenApply(opt -> opt.map(g -> g.getCachedData().getMetaData()).orElse(null));
        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404).result("Group doesn't exist");
            } else {
                ctx.json(result);
            }
        });
    }

    // GET /group/{id}/permissionCheck
    @Override
    public void permissionCheck(Context ctx) {
        String name = ctx.pathParam("id");
        String permission = ctx.queryParam("permission");
        if (permission == null || permission.isEmpty()) {
            throw new IllegalArgumentException("Missing permission");
        }

        CompletableFuture<PermissionCheckResult> future = this.groupManager.loadGroup(name)
                .thenApply(opt -> opt.map(group ->
                        PermissionCheckResult.from(group.getCachedData().getPermissionData().queryPermission(permission)))
                        .orElse(null)
                );

        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404).result("Group doesn't exist");
            } else {
                ctx.json(result);
            }
        });
    }

    // POST /group/{id}/permissionCheck
    @Override
    public void permissionCheckCustom(Context ctx) {
        String name = ctx.pathParam("id");
        PermissionCheckRequest req = ctx.bodyAsClass(PermissionCheckRequest.class);
        if (req.permission() == null || req.permission().isEmpty()) {
            throw new IllegalArgumentException("Missing permission");
        }

        CompletableFuture<PermissionCheckResult> future = this.groupManager.loadGroup(name)
                .thenApply(opt -> opt.map(group -> {
                    QueryOptions options = req.queryOptions();
                    if (options == null) {
                        options = group.getQueryOptions();
                    }
                    return PermissionCheckResult.from(group.getCachedData().getPermissionData(options).queryPermission(req.permission()));
                }).orElse(null));

        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(404).result("Group doesn't exist");
            } else {
                ctx.json(result);
            }
        });
    }

    // POST /group/{id}/promote
    @Override
    public void promote(Context ctx) throws Exception {
        throw new UnsupportedOperationException();
    }

    // POST /group/{id}/demote
    @Override
    public void demote(Context ctx) throws Exception {
        throw new UnsupportedOperationException();
    }
}
