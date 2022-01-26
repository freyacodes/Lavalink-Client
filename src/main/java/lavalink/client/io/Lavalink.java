/*
 * Copyright (c) 2017 Frederik Ar. Mikkelsen & NoobLance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package lavalink.client.io;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.java_websocket.drafts.Draft_6455;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Lavalink<T extends Link> {

    private static final Logger log = LoggerFactory.getLogger(Lavalink.class);

    @SuppressWarnings("WeakerAccess")
    protected final int numShards;
    /** User id may be set at a later time */
    @Nullable
    private String userId;
    private final ConcurrentHashMap<String, T> links = new ConcurrentHashMap<>();
    final List<LavalinkSocket> nodes = new CopyOnWriteArrayList<>();
    final LavalinkLoadBalancer loadBalancer = new LavalinkLoadBalancer(this);

    private final ScheduledExecutorService reconnectService;

    public Lavalink(@Nullable String userId, int numShards) {
        this.userId = userId;
        this.numShards = numShards;

        reconnectService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "lavalink-reconnect-thread");
            thread.setDaemon(true);
            return thread;
        });
        reconnectService.scheduleWithFixedDelay(new ReconnectTask(this), 0, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a Lavalink instance.
     * N.B: You must set the user ID before adding a node
     */
    @SuppressWarnings("unused")
    public Lavalink(int numShards) {
        this(null, numShards);
    }

    private static final AtomicInteger nodeCounter = new AtomicInteger(0);

    public LavalinkSocket addNode(@NonNull URI serverUri, @NonNull String password) {
        return addNode(serverUri, password, null);
    }

    public LavalinkSocket addNode(@NonNull URI serverUri, @NonNull String password, @Nullable String resumeKey) {
        return addNode("Lavalink_Node_#" + nodeCounter.getAndIncrement(), serverUri, password, resumeKey);
    }

    public LavalinkSocket addNode(@NonNull String name, @NonNull URI serverUri, @NonNull String password) {
        return addNode(name, serverUri, password, null);
    }

    /**
     *
     * @param name
     *         A name to identify this node. May show up in metrics and other places.
     * @param serverUri
     *         uri of the node to be added
     * @param password
     *         password of the node to be added
     * @param resumeKey
     *        resume key of the node to be added
     * @throws IllegalStateException if no userId has been set.
     * @throws IllegalArgumentException if a node with that name already exists.
     * @see #setUserId(String)
     */
    @SuppressWarnings("WeakerAccess")
    public LavalinkSocket addNode(@NonNull String name, @NonNull URI serverUri, @NonNull String password, @Nullable String resumeKey) {
        if (userId == null) {
            throw new IllegalStateException("We need a userId to connect to Lavalink");
        }

        if (nodes.stream().anyMatch(sock -> sock.getName().equals(name))) {
            throw new IllegalArgumentException("A node with the name " + name + " already exists.");
        }

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", password);
        headers.put("Num-Shards", Integer.toString(numShards));
        headers.put("User-Id", userId);
        headers.put("Client-Name", "Lavalink-Client");
        if (resumeKey != null) {
            headers.put("Resume-Key", resumeKey);
        }

        LavalinkSocket socket = new LavalinkSocket(name, this, serverUri, new Draft_6455(), headers);
        socket.connect();
        nodes.add(socket);
        return socket;
    }

    @SuppressWarnings("unused")
    public void removeNode(int key) {
        LavalinkSocket node = nodes.remove(key);
        node.close();
    }

    @SuppressWarnings("unused")
    @NonNull
    public LavalinkLoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public T getLink(@NonNull String guildId) {
        return links.computeIfAbsent(guildId, __ -> buildNewLink(guildId));
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    public T getExistingLink(@NonNull String guildId) {
        return links.get(guildId);
    }

    /**
     * Hook to build a new Link.
     * Since the Link class is abstract, you will have to return your own implementation of Link.
     *
     * @param guildId the associated guild's ID
     * @return the new link
     */
    protected abstract T buildNewLink(String guildId);

    @SuppressWarnings({"WeakerAccess", "unused"})
    public int getNumShards() {
        return numShards;
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Collection<T> getLinks() {
        return links.values();
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public List<LavalinkSocket> getNodes() {
        return nodes;
    }

    /**
     * The user id of this bot.
     * @throws IllegalStateException if any nodes are registered.
     */
    @SuppressWarnings("unused")
    public void setUserId(@Nullable String userId) {
        if (!nodes.isEmpty()) {
            throw new IllegalStateException("Can't set userId if we already have nodes registered!");
        }
        this.userId = userId;
    }

    public void shutdown() {
        reconnectService.shutdown();
        nodes.forEach(ReusableWebSocket::close);
    }

    void removeDestroyedLink(Link link) {
        log.debug("Destroyed link for guild " + link.getGuildId());
        links.remove(link.getGuildId());
    }

    @SuppressWarnings("WeakerAccess")
    protected Map<String, T> getLinksMap() {
        return links;
    }

}
