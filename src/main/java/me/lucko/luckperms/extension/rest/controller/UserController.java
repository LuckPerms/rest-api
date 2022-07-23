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

import me.lucko.luckperms.extension.rest.model.PermissionCheckRequest;
import me.lucko.luckperms.extension.rest.model.PermissionCheckResult;

import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;

import io.javalin.http.Context;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserController implements PermissionHolderController {

    private final UserManager userManager;
    private final ObjectMapper objectMapper;

    public UserController(UserManager userManager, ObjectMapper objectMapper) {
        this.userManager = userManager;
        this.objectMapper = objectMapper;
    }

    // POST /user
    @Override
    public void create(Context ctx) {
        CreateReq body = ctx.bodyAsClass(CreateReq.class);

        CompletableFuture<User> future = this.userManager.savePlayerData(body.uniqueId, body.username)
                .thenCompose(result -> {
                    if (result.includes(PlayerSaveResult.Outcome.CLEAN_INSERT)) {
                        return this.userManager.loadUser(body.uniqueId, body.username);
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                });

        ctx.future(future, result -> {
            if (result == null) {
                ctx.status(409).result("User already exists!");
            } else {
                ctx.status(201).json(result);
            }
        });
    }

    record CreateReq(@JsonProperty(required = true) UUID uniqueId, String username) { }

    // GET /user
    @Override
    public void getAll(Context ctx) {
        ctx.future(this.userManager.getUniqueUsers());
    }

    // GET /user/{id}
    @Override
    public void get(Context ctx) {
        UUID uniqueId = ctx.pathParamAsClass("id", UUID.class).get();
        ctx.future(this.userManager.loadUser(uniqueId), result -> {
            if (result == null) {
                ctx.status(404);
            } else {
                ctx.json(result);
            }
        });
    }

    // PATCH /user/{id}
    @Override
    public void update(Context ctx) {
        UUID uniqueId = ctx.pathParamAsClass("id", UUID.class).get();
        UpdateReq body = ctx.bodyAsClass(UpdateReq.class);
        ctx.future(this.userManager.savePlayerData(uniqueId, body.username), result -> ctx.result("ok"));
    }

    record UpdateReq(String username) { }

    // DELETE /user/{id}
    @Override
    public void delete(Context ctx) {
        UUID uniqueId = ctx.pathParamAsClass("id", UUID.class).get();
        CompletableFuture<?> future = this.userManager.modifyUser(uniqueId, user -> user.data().clear())
                .thenCompose(x -> this.userManager.deletePlayerData(uniqueId));
        ctx.future(future, result -> ctx.result("ok"));
    }

    // GET /user/{id}/nodes
    @Override
    public void nodesGet(Context ctx) {
        UUID uniqueId = ctx.pathParamAsClass("id", UUID.class).get();
        CompletableFuture<Collection<Node>> future = this.userManager.loadUser(uniqueId).thenApply(PermissionHolder::getNodes);
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
        UUID uniqueId = ctx.pathParamAsClass("id", UUID.class).get();
        List<Node> nodes = this.objectMapper.readValue(ctx.body(), new TypeReference<>(){});

        CompletableFuture<Collection<Node>> future = this.userManager.loadUser(uniqueId).thenCompose(user -> {
            for (Node node : nodes) {
                user.data().add(node);
            }
            return this.userManager.saveUser(user).thenApply(v -> user.getNodes());
        });
        ctx.future(future);
    }


    // DELETE /user/{id}/nodes
    @Override
    public void nodesDeleteAll(Context ctx) {
        UUID uniqueId = ctx.pathParamAsClass("id", UUID.class).get();
        CompletableFuture<?> future = this.userManager.modifyUser(uniqueId, user -> user.data().clear());
        ctx.future(future, result -> ctx.result("ok"));
    }

    // POST /user/{id}/nodes
    @Override
    public void nodesAddSingle(Context ctx) {
        UUID uniqueId = ctx.pathParamAsClass("id", UUID.class).get();
        Node node = ctx.bodyAsClass(Node.class);

        CompletableFuture<Collection<Node>> future = this.userManager.loadUser(uniqueId).thenCompose(user -> {
            user.data().add(node);
            return this.userManager.saveUser(user).thenApply(v -> user.getNodes());
        });
        ctx.future(future);
    }

    // PUT /user/{id}/nodes
    @Override
    public void nodesSet(Context ctx) throws JsonProcessingException {
        UUID uniqueId = ctx.pathParamAsClass("id", UUID.class).get();
        List<Node> nodes = this.objectMapper.readValue(ctx.body(), new TypeReference<>(){});

        CompletableFuture<Collection<Node>> future = this.userManager.loadUser(uniqueId).thenCompose(user -> {
            user.data().clear();
            for (Node node : nodes) {
                user.data().add(node);
            }
            return this.userManager.saveUser(user).thenApply(v -> user.getNodes());
        });
        ctx.future(future);
    }

    // GET /user/{id}/meta
    @Override
    public void metaGet(Context ctx) {
        UUID uniqueId = ctx.pathParamAsClass("id", UUID.class).get();
        CompletableFuture<CachedMetaData> future = this.userManager.loadUser(uniqueId)
                .thenApply(user -> user.getCachedData().getMetaData());
        ctx.future(future);
    }

    // GET /user/{id}/permissionCheck
    @Override
    public void permissionCheck(Context ctx) {
        UUID uniqueId = ctx.pathParamAsClass("id", UUID.class).get();
        String permission = ctx.queryParam("permission");
        if (permission == null) {
            throw new IllegalArgumentException("Missing permission");
        }

        CompletableFuture<PermissionCheckResult> future = this.userManager.loadUser(uniqueId)
                .thenApply(user -> user.getCachedData().getPermissionData().queryPermission(permission))
                .thenApply(PermissionCheckResult::from);

        ctx.future(future);
    }

    // POST /user/{id}/permissionCheck
    @Override
    public void permissionCheckCustom(Context ctx) {
        UUID uniqueId = ctx.pathParamAsClass("id", UUID.class).get();
        PermissionCheckRequest req = ctx.bodyAsClass(PermissionCheckRequest.class);

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
}
