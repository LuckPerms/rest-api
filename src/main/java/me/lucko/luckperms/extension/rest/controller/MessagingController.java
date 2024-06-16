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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MessagingController {

    private final MessagingService messagingService;
    private final UserManager userManager;
    private final ObjectMapper objectMapper;

    public MessagingController(MessagingService messagingService, UserManager userManager, ObjectMapper objectMapper) {
        this.messagingService = messagingService;
        this.userManager = userManager;
        this.objectMapper = objectMapper;
    }

    private UUID parseUuid(String s) throws JsonProcessingException {
        String uuidString = "\"" + s + "\"";
        return this.objectMapper.readValue(uuidString, UUID.class);
    }

    private UUID pathParamAsUuid(Context ctx) throws JsonProcessingException {
        return parseUuid(ctx.pathParam("id"));
    }

    // POST /update
    public void update(Context ctx) {
        if (this.messagingService == null) {
            ctx.status(501).result("messaging service not available");
            return;
        }

        this.messagingService.pushUpdate();
        ctx.status(202).result("ok");
    }

    // POST /update/{id}
    public void updateUser(Context ctx) throws JsonProcessingException {
        if (this.messagingService == null) {
            ctx.status(501).result("messaging service not available");
            return;
        }

        UUID uniqueId = pathParamAsUuid(ctx);

        User u = this.userManager.getUser(uniqueId);
        CompletableFuture<User> userFuture = u != null
                ? CompletableFuture.completedFuture(u)
                : this.userManager.loadUser(uniqueId);

        ctx.future(userFuture.thenAccept(user -> {
            if (user != null) {
                this.messagingService.pushUserUpdate(user);
            }
        }), result -> ctx.status(202).result("ok"));
    }

    // POST /custom
    public void custom(Context ctx) {
        if (this.messagingService == null) {
            ctx.status(501).result("messaging service not available");
            return;
        }

        CustomMessageReq body = ctx.bodyAsClass(CustomMessageReq.class);
        this.messagingService.sendCustomMessage(body.channelId(), body.payload());
        ctx.status(202).result("ok");
    }

    record CustomMessageReq(
            @JsonProperty(required = true) String channelId,
            @JsonProperty(required = true) String payload
    ) { }

}
