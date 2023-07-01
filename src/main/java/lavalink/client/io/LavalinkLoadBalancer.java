/*
 * Copyright (c) Freya Arbjerg. Licensed under the MIT license
 */

package lavalink.client.io;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class LavalinkLoadBalancer {

    private Lavalink lavalink;
    //private Map<String, Optional<LavalinkSocket>> socketMap = new ConcurrentHashMap<>();
    private List<PenaltyProvider> penaltyProviders = new ArrayList<>();

    LavalinkLoadBalancer(Lavalink lavalink) {
        this.lavalink = lavalink;
    }

    @NonNull
    public LavalinkSocket determineBestSocket(long guild) {
        LavalinkSocket leastPenalty = null;
        int record = Integer.MAX_VALUE;

        @SuppressWarnings("unchecked")
        List<LavalinkSocket> nodes = lavalink.getNodes();
        for (LavalinkSocket socket : nodes) {
            int total = getPenalties(socket, guild, penaltyProviders).getTotal();
            if (total < record) {
                leastPenalty = socket;
                record = total;
            }
        }

        if (leastPenalty == null || !leastPenalty.isAvailable())
            throw new IllegalStateException("No available nodes!");

        return leastPenalty;
    }

    @SuppressWarnings("unused")
    public void addPenalty(PenaltyProvider penalty) {
        this.penaltyProviders.add(penalty);
    }

    @SuppressWarnings("unused")
    public void removePenalty(PenaltyProvider penalty) {
        this.penaltyProviders.remove(penalty);
    }

    void onNodeDisconnect(LavalinkSocket disconnected) {
        //noinspection unchecked
        Collection<Link> links = lavalink.getLinks();
        links.forEach(link -> {
            if (disconnected.equals(link.getNode(false)))
                link.changeNode(lavalink.loadBalancer.determineBestSocket(link.getGuildIdLong()));
        });
    }

    void onNodeConnect(LavalinkSocket connected) {
        @SuppressWarnings("unchecked")
        List<LavalinkSocket> sockets = lavalink.getNodes();
        long otherAvailableNodes = sockets.stream()
                .filter(node -> node != connected)
                .filter(LavalinkSocket::isAvailable)
                .count();
        if (otherAvailableNodes > 0) { //only update links if this is the only connected node
            return;
        }
        @SuppressWarnings("unchecked")
        Collection<Link> links = lavalink.getLinks();
        links.forEach(link -> {
            if (link.getNode(false) == null)
                link.changeNode(connected);
        });
    }

    public Penalties getPenalties(LavalinkSocket socket, long guild, List<PenaltyProvider> penaltyProviders) {
        return new Penalties(socket, guild, penaltyProviders, lavalink);
    }

    @SuppressWarnings("unused")
    public static Penalties getPenalties(LavalinkSocket socket) {
        return new Penalties(socket, 0L, Collections.emptyList(), null);
    }

    @SuppressWarnings("unused")
    public static class Penalties {

        private LavalinkSocket socket;
        private final long guild;
        private int playerPenalty = 0;
        private int cpuPenalty = 0;
        private int deficitFramePenalty = 0;
        private int nullFramePenalty = 0;
        private int customPenalties = 0;
        private final Lavalink lavalink;

        private Penalties(LavalinkSocket socket, long guild, List<PenaltyProvider> penaltyProviders, Lavalink lavalink) {
            this.lavalink = lavalink;
            this.socket = socket;
            this.guild = guild;
            RemoteStats stats = socket.getStats();
            if (stats == null) return; // Will return as max penalty anyways
            // This will serve as a rule of thumb. 1 playing player = 1 penalty point
            if (lavalink != null) {
                playerPenalty = countPlayingPlayers();
            } else {
                playerPenalty = stats.getPlayingPlayers();
            }

            // https://fred.moe/293.png
            cpuPenalty = (int) Math.pow(1.05d, 100 * stats.getSystemLoad()) * 10 - 10;

            // -1 Means we don't have any frame stats. This is normal for very young nodes
            if (stats.getAvgFramesDeficitPerMinute() != -1) {
                // https://fred.moe/rjD.png
                deficitFramePenalty = (int) (Math.pow(1.03d, 500f * ((float) stats.getAvgFramesDeficitPerMinute() / 3000f)) * 600 - 600);
                nullFramePenalty = (int) (Math.pow(1.03d, 500f * ((float) stats.getAvgFramesNulledPerMinute() / 3000f)) * 300 - 300);
                nullFramePenalty *= 2;
                // Deficit frames are better than null frames, as deficit frames can be caused by the garbage collector
            }
            penaltyProviders.forEach(pp -> customPenalties += pp.getPenalty(this));
        }

        private int countPlayingPlayers() {
            //noinspection unchecked
            Collection<Link> links = lavalink.getLinks();
            Long players = links
                    .stream().filter(link ->
                            socket.equals(link.getNode(false)) &&
                                    link.getPlayer().getPlayingTrack() != null &&
                                    !link.getPlayer().isPaused())
                    .count();
            return players.intValue();
        }

        public LavalinkSocket getSocket() {
            return socket;
        }

        public long getGuild() {
            return guild;
        }

        public int getPlayerPenalty() {
            return playerPenalty;
        }

        public int getCpuPenalty() {
            return cpuPenalty;
        }

        public int getDeficitFramePenalty() {
            return deficitFramePenalty;
        }

        public int getNullFramePenalty() {
            return nullFramePenalty;
        }

        public int getCustomPenalties() {
            return this.customPenalties;
        }

        public int getTotal() {
            if (!socket.isAvailable() || socket.getStats() == null) return (Integer.MAX_VALUE - 1);
            return playerPenalty + cpuPenalty + deficitFramePenalty + nullFramePenalty + customPenalties;
        }

        @Override
        public String toString() {
            if (!socket.isAvailable()) return "Penalties{" +
                    "unavailable=" + (Integer.MAX_VALUE - 1) +
                    '}';

            return "Penalties{" +
                    "total=" + getTotal() +
                    ", playerPenalty=" + playerPenalty +
                    ", cpuPenalty=" + cpuPenalty +
                    ", deficitFramePenalty=" + deficitFramePenalty +
                    ", nullFramePenalty=" + nullFramePenalty +
                    ", custom=" + customPenalties +
                    '}';
        }
    }

}
