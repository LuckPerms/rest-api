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

import io.javalin.http.sse.SseClient;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.LuckPermsEvent;
import net.luckperms.api.event.log.LogBroadcastEvent;
import net.luckperms.api.event.messaging.CustomMessageReceiveEvent;
import net.luckperms.api.event.sync.PostNetworkSyncEvent;
import net.luckperms.api.event.sync.PostSyncEvent;
import net.luckperms.api.event.sync.PreNetworkSyncEvent;
import net.luckperms.api.event.sync.PreSyncEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class EventController implements AutoCloseable {

    private final EventBus eventBus;
    private final Set<SseClient> clients;
    private final AtomicLong pingCounter;

    private final ScheduledExecutorService executor;

    public EventController(EventBus eventBus) {
        this.eventBus = eventBus;
        this.clients = ConcurrentHashMap.newKeySet();
        this.pingCounter = new AtomicLong();

        AtomicInteger threadCount = new AtomicInteger(0);
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setName("luckperms-rest-event-controller-" + threadCount.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        this.executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        this.executor.scheduleAtFixedRate(this::tick, 10, 10, TimeUnit.SECONDS);
    }

    public void tick() {
        long pingId = this.pingCounter.incrementAndGet();
        for (SseClient client : this.clients) {
            client.sendEvent("ping", pingId);
        }
    }

    @Override
    public void close() throws Exception {
        this.executor.shutdown();
        for (SseClient client : this.clients) {
            client.close();
        }
    }

    private void handle(SseClient client, Class<? extends LuckPermsEvent> eventClass) {
        this.clients.add(client);
        // Keep the SSE connection alive
        client.keepAlive();
        EventSubscription<?> subscription = this.eventBus.subscribe(eventClass, client::sendEvent);
        client.onClose(() -> {
            subscription.close();
            this.clients.remove(client);
        });

    }

    // GET /log-broadcast
    public void logBroadcast(SseClient client) {
        handle(client, LogBroadcastEvent.class);
    }

    // GET /post-network-sync
    public void postNetworkSync(SseClient client) {
        handle(client, PostNetworkSyncEvent.class);
    }

    // GET /post-sync
    public void postSync(SseClient client) {
        handle(client, PostSyncEvent.class);
    }

    // GET /pre-network-sync
    public void preNetworkSync(SseClient client) {
        handle(client, PreNetworkSyncEvent.class);
    }

    // GET /pre-sync
    public void preSync(SseClient client) {
        handle(client, PreSyncEvent.class);
    }

    // GET /custom-message-receive
    public void customMessageReceive(SseClient client) {
        handle(client, CustomMessageReceiveEvent.class);
    }

}
