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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import me.lucko.luckperms.extension.rest.model.ActionPage;
import me.lucko.luckperms.extension.rest.model.ActionRequest;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.actionlog.ActionLogger;
import net.luckperms.api.actionlog.filter.ActionFilter;

import java.util.concurrent.CompletableFuture;

public class ActionController {

    private final ActionLogger actionLogger;
    private final ObjectMapper objectMapper;

    public ActionController(ActionLogger actionLogger, ObjectMapper objectMapper) {
        this.actionLogger = actionLogger;
        this.objectMapper = objectMapper;
    }

    // GET /action
    public void get(Context ctx) throws JsonProcessingException {
        ActionFilter filter = ActionRequest.parseFilter(this.objectMapper, ctx);

        Integer pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(null);
        Integer pageNumber = ctx.queryParamAsClass("pageNumber", Integer.class).getOrDefault(null);

        if (pageSize == null && pageNumber == null) {
            CompletableFuture<ActionPage> future = this.actionLogger.queryActions(filter)
                    .thenApply(list -> new ActionPage(list, list.size()));
            ctx.future(future);
        } else {
            if (pageSize == null) {
                ctx.status(400).result("pageSize query parameter is required when pageNumber is provided");
                return;
            } else if (pageNumber == null) {
                ctx.status(400).result("pageNumber query parameter is required when pageSize is provided");
                return;
            }

            CompletableFuture<ActionPage> future = this.actionLogger.queryActions(filter, pageSize, pageNumber)
                    .thenApply(ActionPage::from);
            ctx.future(future);
        }
    }

    // POST /action
    public void submit(Context ctx) {
        Action req = ctx.bodyAsClass(Action.class);

        CompletableFuture<Void> future = this.actionLogger.submit(req);
        ctx.future(future, result -> ctx.status(202).result("ok"));
    }

}
