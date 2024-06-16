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

import io.javalin.http.Context;

public interface PermissionHolderController {

    // POST /<type>
    void create(Context ctx) throws Exception;

    // GET /<type>
    void getAll(Context ctx) throws Exception;

    // GET /<type>/search
    void search(Context ctx) throws Exception;

    // GET /<type>/{id}
    void get(Context ctx) throws Exception;

    // PATCH /<type>/{id}
    void update(Context ctx) throws Exception;

    // DELETE /<type>/{id}
    void delete(Context ctx) throws Exception;

    // GET /<type>/{id}/nodes
    void nodesGet(Context ctx) throws Exception;

    // PATCH /<type>/{id}/nodes
    void nodesAddMultiple(Context ctx) throws Exception;

    // DELETE /<type>/{id}/nodes
    void nodesDeleteAll(Context ctx) throws Exception;

    // POST /<type>/{id}/nodes
    void nodesAddSingle(Context ctx) throws Exception;

    // PUT /<type>/{id}/nodes
    void nodesSet(Context ctx) throws Exception;

    // GET /<type>/{id}/meta
    void metaGet(Context ctx) throws Exception;

    // GET /<type>/{id}/permission-check
    void permissionCheck(Context ctx) throws Exception;

    // POST /<type>/{id}/permission-check
    void permissionCheckCustom(Context ctx) throws Exception;

    // POST /<type>/{id}/promote
    void promote(Context ctx) throws Exception;

    // POST /<type>/{id}/demote
    void demote(Context ctx) throws Exception;

}
