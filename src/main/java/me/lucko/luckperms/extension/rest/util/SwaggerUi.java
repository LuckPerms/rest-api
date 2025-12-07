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

import io.javalin.apibuilder.ApiBuilder;
import io.javalin.config.JavalinConfig;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import me.lucko.luckperms.extension.rest.RestServer;

import java.util.Objects;

public class SwaggerUi {

    public static void setup(JavalinConfig config) {
        // Serve our custom OpenAPI YAML specification
        config.router.apiBuilder(() -> {
            ApiBuilder.get("/docs/openapi", ctx ->
                    ctx.result(Objects.requireNonNull(RestServer.class.getClassLoader().getResourceAsStream("luckperms-openapi.yml")))
                            .contentType("application/x-yaml")
            );
        });

        // Register Swagger UI plugin with custom OpenAPI spec injection
        config.registerPlugin(new SwaggerPlugin(swaggerConfig -> {
            swaggerConfig.setUiPath("/docs/swagger-ui");
            // Inject our custom OpenAPI spec URL
            swaggerConfig.injectCustomVersion("default", "/docs/openapi");
        }));
    }

}
