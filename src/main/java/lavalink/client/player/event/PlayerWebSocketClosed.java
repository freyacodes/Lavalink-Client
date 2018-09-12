package lavalink.client.player.event;

import lavalink.client.player.IPlayer;

public class PlayerWebSocketClosed extends PlayerEvent {
    private final int code;
    private final String reason;
    private final boolean byRemote;

    public PlayerWebSocketClosed(IPlayer player, int code, String reason, boolean byRemote) {
        super(player);
        this.code = code;
        this.reason = reason;
        this.byRemote = byRemote;
    }

    public int getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    public boolean byRemote() {
        return byRemote;
    }
}
