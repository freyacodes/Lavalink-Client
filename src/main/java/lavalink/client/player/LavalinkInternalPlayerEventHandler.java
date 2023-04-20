/*
 * Copyright (c) Freya Arbjerg. Licensed under the MIT license
 */

package lavalink.client.player;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.player.event.PlayerEventListenerAdapter;

class LavalinkInternalPlayerEventHandler extends PlayerEventListenerAdapter {

    @Override
    public void onTrackEnd(IPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason != AudioTrackEndReason.REPLACED && endReason != AudioTrackEndReason.STOPPED) {
            ((LavalinkPlayer) player).clearTrack();
        }
    }
}
