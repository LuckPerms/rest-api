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

package me.lucko.luckperms.extension.rest;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.google.common.collect.ImmutableSet;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.core.util.JavalinLogger;
import io.javalin.http.HttpCode;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.openapi.utils.OpenApiVersionUtil;
import me.lucko.luckperms.extension.rest.controller.ActionController;
import me.lucko.luckperms.extension.rest.controller.GroupController;
import me.lucko.luckperms.extension.rest.controller.PermissionHolderController;
import me.lucko.luckperms.extension.rest.controller.UserController;
import me.lucko.luckperms.extension.rest.util.CustomObjectMapper;
import me.lucko.luckperms.extension.rest.util.StubMessagingService;
import me.lucko.luckperms.extension.rest.util.SwaggerUi;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.platform.Health;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

/**
 * An HTTP server that implements a REST API for LuckPerms.
 */
public class RestServer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestServer.class);

    private final ObjectMapper objectMapper;
    private final Javalin app;

    public RestServer(LuckPerms luckPerms, int port) {
        LOGGER.info("[REST] Starting server...");

        this.objectMapper = new CustomObjectMapper();

        this.app = Javalin.create(this::configure)
                .start(port);

        this.setupLogging(this.app);
        this.setupErrorHandlers(this.app);
        this.setupRoutes(this.app, luckPerms);

        LOGGER.info("[REST] Startup complete! Listening on http://localhost:" + port);
    }

    @Override
    public void close() {
        this.app.close();
    }

    private void configure(JavalinConfig config) {
        // disable javalin excessive logging
        config.showJavalinBanner = false;
        JavalinLogger.enabled = false;
        JavalinLogger.startupInfo = false;
        OpenApiVersionUtil.INSTANCE.setLogWarnings(false);

        this.setupAuth(config);

        SwaggerUi.setup(config);

        config.jsonMapper(new JavalinJackson(this.objectMapper));
    }

    private void setupErrorHandlers(Javalin app) {
        app.exception(MismatchedInputException.class, (e, ctx) -> ctx.status(400).result(e.getMessage()));
        app.exception(JacksonException.class, (e, ctx) -> ctx.status(400).result(e.getMessage()));
        app.exception(IllegalArgumentException.class, (e, ctx) -> ctx.status(400).result(e.getMessage()));
        app.exception(UnsupportedOperationException.class, (e, ctx) -> ctx.status(404).result("Not found"));

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500).result("Server error");
            LOGGER.error("Server error while handing request", e);
        });
    }

    private void setupRoutes(Javalin app, LuckPerms luckPerms) {
        app.get("/", ctx -> ctx.redirect("/docs/swagger-ui"));

        app.get("health", ctx -> {
            Health health = luckPerms.runHealthCheck();
            ctx.status(health.isHealthy() ? HttpCode.OK : HttpCode.SERVICE_UNAVAILABLE).json(health);
        });

        MessagingService messagingService = luckPerms.getMessagingService().orElse(StubMessagingService.INSTANCE);

        UserController userController = new UserController(luckPerms.getUserManager(), luckPerms.getTrackManager(), messagingService, this.objectMapper);
        GroupController groupController = new GroupController(luckPerms.getGroupManager(), messagingService, this.objectMapper);
        ActionController actionController = new ActionController(luckPerms.getActionLogger());

        app.routes(() -> {
            path("user", () -> {
                get("lookup", userController::lookup);
                setupControllerRoutes(userController);
            });
            path("group", () -> {
                setupControllerRoutes(groupController);
            });
            path("action", () -> {
                post(actionController::submit);
            });
        });
    }

    private void setupControllerRoutes(PermissionHolderController controller) {
        post(controller::create);
        get(controller::getAll);

        get("search", controller::search);

        path("{id}", () -> {
            get(controller::get);
            patch(controller::update);
            delete(controller::delete);

            path("nodes", () -> {
                get(controller::nodesGet);
                patch(controller::nodesAddMultiple);
                delete(controller::nodesDeleteAll);
                post(controller::nodesAddSingle);
                put(controller::nodesSet);
            });

            get("meta", controller::metaGet);

            path("permissionCheck", () -> {
                get(controller::permissionCheck);
                post(controller::permissionCheckCustom);
            });

            post("promote", controller::promote);
            post("demote", controller::demote);
        });
    }

    private void setupAuth(JavalinConfig config) {
        if (RestConfig.getBoolean("auth", false)) {
            Set<String> keys = ImmutableSet.copyOf(
                    RestConfig.getStringList("auth.keys", Collections.emptyList())
            );

            if (keys.isEmpty()) {
                LOGGER.warn("[REST] Auth is enabled but there are no API keys registered!");
                LOGGER.warn("[REST] Set some keys with the 'LUCKPERMS_REST_AUTH_KEYS' variable.");
            }

            config.accessManager((handler, ctx, routeRoles) -> {
                if (ctx.path().equals("/") || ctx.path().startsWith("/docs")) {
                    handler.handle(ctx);
                    return;
                }

                String authorization = ctx.header("Authorization");
                if (authorization == null) {
                    ctx.status(HttpCode.UNAUTHORIZED).result("No API key");
                    return;
                }

                String[] parts = authorization.split(" ");
                if (parts.length != 2) {
                    ctx.status(HttpCode.UNAUTHORIZED).result("Invalid API key");
                    return;
                }

                if (!parts[0].equals("Bearer")) {
                    ctx.status(HttpCode.UNAUTHORIZED).result("Unknown Authorization type");
                    return;
                }

                if (!keys.contains(parts[1])) {
                    ctx.status(HttpCode.UNAUTHORIZED).result("Unauthorized");
                    return;
                }

                handler.handle(ctx);
            });
        }
    }

    private void setupLogging(Javalin app) {
        app.before(ctx -> ctx.attribute("startTime", System.currentTimeMillis()));
        app.after(ctx -> {
            //noinspection ConstantConditions
            long startTime = ctx.attribute("startTime");
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("[REST] %s %s - %d - %dms".formatted(ctx.method(), ctx.path(), ctx.status(), duration));
        });
    }

}
