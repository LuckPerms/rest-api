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

import net.luckperms.api.LuckPerms;
import net.luckperms.api.extension.Extension;

public class RestExtension implements Extension {

    private final LuckPerms luckPerms;
    private RestServer server;

    public RestExtension(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    @Override
    public void load() {
        String port = System.getProperty("luckperms.rest.api.port", System.getenv("LUCKPERMS_REST_API_PORT"));
        if (port == null) {
            port = "8080";
        }

        Thread thread = Thread.currentThread();
        ClassLoader previousCtxClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(RestExtension.class.getClassLoader());
        try {
            this.server = new RestServer(this.luckPerms, Integer.parseInt(port));
        } finally {
            thread.setContextClassLoader(previousCtxClassLoader);
        }
    }

    @Override
    public void unload() {
        if (this.server != null) {
            this.server.close();
            this.server = null;
        }
    }
}
