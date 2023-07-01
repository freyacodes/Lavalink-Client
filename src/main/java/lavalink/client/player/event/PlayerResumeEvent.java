/*
 * Copyright (c) Freya Arbjerg. Licensed under the MIT license
 */

package lavalink.client.player.event;

import lavalink.client.player.IPlayer;

public class PlayerResumeEvent extends PlayerEvent {
    public PlayerResumeEvent(IPlayer player) {
        super(player);
    }
}
