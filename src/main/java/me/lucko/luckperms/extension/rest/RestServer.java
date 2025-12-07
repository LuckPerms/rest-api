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
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import me.lucko.luckperms.extension.rest.controller.ActionController;
import me.lucko.luckperms.extension.rest.controller.EventController;
import me.lucko.luckperms.extension.rest.controller.GroupController;
import me.lucko.luckperms.extension.rest.controller.MessagingController;
import me.lucko.luckperms.extension.rest.controller.PermissionHolderController;
import me.lucko.luckperms.extension.rest.controller.TrackController;
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
import static io.javalin.apibuilder.ApiBuilder.sse;

/**
 * An HTTP server that implements a REST API for LuckPerms.
 */
public class RestServer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestServer.class);

    private final ObjectMapper objectMapper;
    private final Javalin app;
    private final EventController eventController;

    public RestServer(LuckPerms luckPerms, String address, int port) {
        LOGGER.info("[REST] Starting server...");

        this.objectMapper = new CustomObjectMapper();

        MessagingService messagingService = luckPerms.getMessagingService().orElse(StubMessagingService.INSTANCE);

        UserController userController = new UserController(luckPerms.getUserManager(), luckPerms.getTrackManager(), messagingService, this.objectMapper);
        GroupController groupController = new GroupController(luckPerms.getGroupManager(), messagingService, this.objectMapper);
        TrackController trackController = new TrackController(luckPerms.getTrackManager(), luckPerms.getGroupManager(), messagingService, this.objectMapper);
        ActionController actionController = new ActionController(luckPerms.getActionLogger(), this.objectMapper);
        MessagingController messagingController = new MessagingController(luckPerms.getMessagingService().orElse(null), luckPerms.getUserManager(), this.objectMapper);
        this.eventController = new EventController(luckPerms.getEventBus());

        this.app = Javalin.create(config -> this.configure(config, luckPerms, userController, groupController, trackController, actionController, messagingController, this.eventController))
                .start(address, port);

        this.setupLogging(this.app);
        this.setupErrorHandlers(this.app);

        LOGGER.info("[REST] Startup complete! Listening on http://{}:{}", address == null ? "localhost" : address, port);
    }

    @Override
    public void close() {
        try {
            this.eventController.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.app.stop();
    }

    private void configure(JavalinConfig config, LuckPerms luckPerms, UserController userController, GroupController groupController, TrackController trackController, ActionController actionController, MessagingController messagingController, EventController eventController) {
        // disable javalin excessive logging
        config.showJavalinBanner = false;

        // Enable webjars for Swagger UI assets
        config.staticFiles.enableWebjars();

        this.setupAuth(config);

        SwaggerUi.setup(config);

        config.jsonMapper(new JavalinJackson(this.objectMapper, true));

        // Setup routes
        config.router.apiBuilder(() -> {
            get("/", ctx -> ctx.redirect("/docs/swagger-ui"));

            get("health", ctx -> {
                Health health = luckPerms.runHealthCheck();
                ctx.status(health.isHealthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).json(health);
            });

            path("user", () -> {
                get("lookup", userController::lookup);
                setupControllerRoutes(userController);
            });
            path("group", () -> setupControllerRoutes(groupController));
            path("track", () -> setupControllerRoutes(trackController));
            path("action", () -> setupControllerRoutes(actionController));
            path("messaging", () -> setupControllerRoutes(messagingController));
            path("event", () -> setupControllerRoutes(eventController));
        });
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

            path("permission-check", () -> {
                get(controller::permissionCheck);
                post(controller::permissionCheckCustom);
            });
            path("permissioncheck", () -> {
                get(controller::permissionCheck);
                post(controller::permissionCheckCustom);
            });

            post("promote", controller::promote);
            post("demote", controller::demote);
        });
    }

    private void setupControllerRoutes(TrackController controller) {
        post(controller::create);
        get(controller::getAll);

        path("{id}", () -> {
            get(controller::get);
            patch(controller::update);
            delete(controller::delete);
        });
    }

    private void setupControllerRoutes(ActionController controller) {
        get(controller::get);
        post(controller::submit);
    }

    private void setupControllerRoutes(MessagingController controller) {
        path("update", () -> {
            post(controller::update);
            post("{id}", controller::updateUser);
        });
        post("custom", controller::custom);
    }

    private void setupControllerRoutes(EventController controller) {
        sse("log-broadcast", controller::logBroadcast);
        sse("post-network-sync", controller::postNetworkSync);
        sse("post-sync", controller::postSync);
        sse("pre-network-sync", controller::preNetworkSync);
        sse("pre-sync", controller::preSync);
        sse("custom-message-receive", controller::customMessageReceive);
    }

    private void setupAuth(JavalinConfig config) {
        if (RestConfig.getBoolean("auth", false)) {
            Set<String> keys = Set.copyOf(
                    RestConfig.getStringList("auth.keys", Collections.emptyList())
            );

            if (keys.isEmpty()) {
                LOGGER.warn("[REST] Auth is enabled but there are no API keys registered!");
                LOGGER.warn("[REST] Set some keys with the 'LUCKPERMS_REST_AUTH_KEYS' variable.");
            }

            config.router.mount(router -> {
                router.beforeMatched(ctx -> {
                    // Skip auth for root, docs, and webjars (Swagger UI assets)
                    String path = ctx.path();
                    if (path.equals("/") || path.startsWith("/docs") || path.startsWith("/webjars")) {
                        return;
                    }

                    String authorization = ctx.header("Authorization");
                    if (authorization == null) {
                        ctx.status(HttpStatus.UNAUTHORIZED).result("No API key");
                        ctx.skipRemainingHandlers();
                        return;
                    }

                    String[] parts = authorization.split(" ");
                    if (parts.length != 2) {
                        ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key");
                        ctx.skipRemainingHandlers();
                        return;
                    }

                    if (!parts[0].equals("Bearer")) {
                        ctx.status(HttpStatus.UNAUTHORIZED).result("Unknown Authorization type");
                        ctx.skipRemainingHandlers();
                        return;
                    }

                    if (!keys.contains(parts[1])) {
                        ctx.status(HttpStatus.UNAUTHORIZED).result("Unauthorized");
                        ctx.skipRemainingHandlers();
                        return;
                    }
                });
            });
        }
    }

    private void setupLogging(Javalin app) {
        app.before(ctx -> {
            ctx.attribute("startTime", System.currentTimeMillis());
            if (ctx.path().startsWith("/event/")) {
                LOGGER.info("[REST] {} {} - {}", ctx.method(), ctx.path(), ctx.status());
            }
        });
        app.after(ctx -> {
            //noinspection ConstantConditions
            long startTime = ctx.attribute("startTime");
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("[REST] {} {} - {} - {}ms", ctx.method(), ctx.path(), ctx.status(), duration);
        });
    }

}
