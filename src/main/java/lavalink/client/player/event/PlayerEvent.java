/*
 * Copyright (c) Freya Arbjerg. Licensed under the MIT license
 */

package lavalink.client.player.event;

import lavalink.client.player.IPlayer;

public abstract class PlayerEvent {

    private final IPlayer player;

    public PlayerEvent(IPlayer player) {
        this.player = player;
    }

    public IPlayer getPlayer() {
        return player;
    }
}
