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
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.module.SimpleModule;

import me.lucko.luckperms.extension.rest.bind.ContextSetDeserializer;
import me.lucko.luckperms.extension.rest.bind.ContextSetSerializer;
import me.lucko.luckperms.extension.rest.bind.GroupSerializer;
import me.lucko.luckperms.extension.rest.bind.MetadataSerializer;
import me.lucko.luckperms.extension.rest.bind.NodeDeserializer;
import me.lucko.luckperms.extension.rest.bind.NodeSerializer;
import me.lucko.luckperms.extension.rest.bind.QueryOptionsDeserializer;
import me.lucko.luckperms.extension.rest.bind.UserSerializer;
import me.lucko.luckperms.extension.rest.controller.GroupController;
import me.lucko.luckperms.extension.rest.controller.PermissionHolderController;
import me.lucko.luckperms.extension.rest.controller.UserController;
import me.lucko.luckperms.extension.rest.util.SwaggerUi;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.core.util.JavalinLogger;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.openapi.utils.OpenApiVersionUtil;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

public class RestServer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestServer.class);

    private final ObjectMapper objectMapper;
    private final Javalin app;

    public RestServer(LuckPerms luckPerms, int port) {
        LOGGER.info("[REST API] Starting server...");

        this.objectMapper = createObjectMapper();

        this.app = Javalin.create(this::configure)
                .start(port);

        this.setupErrorHandlers(this.app);
        this.setupRoutes(this.app, luckPerms);

        LOGGER.info("[REST API] Startup complete! Listening on :" + port);
    }

    @Override
    public void close() {
        this.app.close();
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        //noinspection deprecation
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(ContextSet.class, new ContextSetDeserializer());
        module.addSerializer(ContextSet.class, new ContextSetSerializer());
        module.addSerializer(Group.class, new GroupSerializer());
        module.addSerializer(CachedMetaData.class, new MetadataSerializer());
        module.addDeserializer(Node.class, new NodeDeserializer());
        module.addSerializer(Node.class, new NodeSerializer());
        module.addDeserializer(QueryOptions.class, new QueryOptionsDeserializer());
        module.addSerializer(User.class, new UserSerializer());
        mapper.registerModule(module);

        return mapper;
    }

    public void configure(JavalinConfig config) {
        config.showJavalinBanner = false;
        JavalinLogger.startupInfo = false;
        OpenApiVersionUtil.INSTANCE.setLogWarnings(false);
        SwaggerUi.setup(config);
        config.jsonMapper(new JavalinJackson(this.objectMapper));
    }

    public void setupErrorHandlers(Javalin app) {
        app.exception(MismatchedInputException.class, (e, ctx) -> ctx.status(400).result(e.getMessage()));
        app.exception(JacksonException.class, (e, ctx) -> ctx.status(400).result(e.getMessage()));
        app.exception(IllegalArgumentException.class, (e, ctx) -> ctx.status(400).result(e.getMessage()));

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500).result("Server error");
            LOGGER.error("Server error while handing request", e);
        });
    }

    public void setupRoutes(Javalin app, LuckPerms luckPerms) {

        app.get("/", ctx -> ctx.redirect("/docs/swagger-ui"));

        UserController userController = new UserController(luckPerms.getUserManager(), this.objectMapper);
        GroupController groupController = new GroupController(luckPerms.getGroupManager(), this.objectMapper);

        app.routes(() -> {
            path("user", () -> setupControllerRoutes(userController));
            path("group", () -> setupControllerRoutes(groupController));
        });
    }

    private void setupControllerRoutes(PermissionHolderController controller) {
        post(controller::create);
        get(controller::getAll);

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
        });
    }

}
