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

import io.javalin.http.Context;

public interface PermissionHolderController {

    // POST /<type>
    void create(Context ctx);

    // GET /<type>
    void getAll(Context ctx);

    // GET /<type>/{id}
    void get(Context ctx);

    // PATCH /<type>/{id}
    void update(Context ctx);

    // DELETE /<type>/{id}
    void delete(Context ctx);

    // GET /<type>/{id}/nodes
    void nodesGet(Context ctx);

    // PATCH /<type>/{id}/nodes
    void nodesAddMultiple(Context ctx) throws JsonProcessingException;

    // DELETE /<type>/{id}/nodes
    void nodesDeleteAll(Context ctx);

    // POST /<type>/{id}/nodes
    void nodesAddSingle(Context ctx);

    // PUT /<type>/{id}/nodes
    void nodesSet(Context ctx) throws JsonProcessingException;

    // GET /<type>/{id}/meta
    void metaGet(Context ctx);

    // GET /<type>/{id}/permissionCheck
    void permissionCheck(Context ctx);

    // POST /<type>/{id}/permissionCheck
    void permissionCheckCustom(Context ctx);

}
