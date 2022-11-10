/*
 * Copyright (c) Freya Arbjerg. Licensed under the MIT license
 */

package lavalink.client.io;

import org.json.JSONObject;

@SuppressWarnings({"unused", "WeakerAccess"})
public class RemoteStats {

    private final JSONObject json;
    private final int players;
    private final int playingPlayers;
    private final long uptime; //in millis

    // In bytes
    private final long memFree;
    private final long memUsed;
    private final long memAllocated;
    private final long memReservable;

    private final int cpuCores;
    private final double systemLoad;
    private final double lavalinkLoad;

    private int avgFramesSentPerMinute = -1;
    private int avgFramesNulledPerMinute = -1;
    private int avgFramesDeficitPerMinute = -1;

    RemoteStats(JSONObject json) {
        this.json = json;
        players = json.getInt("players");
        playingPlayers = json.getInt("playingPlayers");
        uptime = json.getLong("uptime");

        memFree = json.getJSONObject("memory").getLong("free");
        memUsed = json.getJSONObject("memory").getLong("used");
        memAllocated = json.getJSONObject("memory").getLong("allocated");
        memReservable = json.getJSONObject("memory").getLong("reservable");

        cpuCores = json.getJSONObject("cpu").getInt("cores");
        systemLoad = json.getJSONObject("cpu").getDouble("systemLoad");
        lavalinkLoad = json.getJSONObject("cpu").getDouble("lavalinkLoad");

        JSONObject frames = json.optJSONObject("frameStats");

        if (frames != null) {
            avgFramesSentPerMinute = frames.getInt("sent");
            avgFramesNulledPerMinute = frames.getInt("nulled");
            avgFramesDeficitPerMinute = frames.getInt("deficit");
        }
    }

    public JSONObject getAsJson() {
        return json;
    }

    public int getPlayers() {
        return players;
    }

    public int getPlayingPlayers() {
        return playingPlayers;
    }

    public long getUptime() { //in millis
        return uptime;
    }

    public long getMemFree() {
        return memFree;
    }

    public long getMemUsed() {
        return memUsed;
    }

    public long getMemAllocated() {
        return memAllocated;
    }

    public long getMemReservable() {
        return memReservable;
    }

    public int getCpuCores() {
        return cpuCores;
    }

    public double getSystemLoad() {
        return systemLoad;
    }

    public double getLavalinkLoad() {
        return lavalinkLoad;
    }

    public int getAvgFramesSentPerMinute() {
        return avgFramesSentPerMinute;
    }

    public int getAvgFramesNulledPerMinute() {
        return avgFramesNulledPerMinute;
    }

    public int getAvgFramesDeficitPerMinute() {
        return avgFramesDeficitPerMinute;
    }

    @Override
    public String toString() {
        return "RemoteStats{" +
                "players=" + players +
                ", playingPlayers=" + playingPlayers +
                ", uptime=" + uptime +
                ", memFree=" + memFree +
                ", memUsed=" + memUsed +
                ", memAllocated=" + memAllocated +
                ", memReservable=" + memReservable +
                ", cpuCores=" + cpuCores +
                ", systemLoad=" + systemLoad +
                ", lavalinkLoad=" + lavalinkLoad +
                ", avgFramesSentPerMinute=" + avgFramesSentPerMinute +
                ", avgFramesNulledPerMinute=" + avgFramesNulledPerMinute +
                ", avgFramesDeficitPerMinute=" + avgFramesDeficitPerMinute +
                '}';
    }
}
