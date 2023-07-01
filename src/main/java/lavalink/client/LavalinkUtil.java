/*
 * Copyright (c) Freya Arbjerg. Licensed under the MIT license
 */

package lavalink.client;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.player.LavalinkPlayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class LavalinkUtil {

    private static final AudioPlayerManager PLAYER_MANAGER;

    static {
        PLAYER_MANAGER = new DefaultAudioPlayerManager();

        /* These are only to encode/decode messages */
        PLAYER_MANAGER.registerSourceManager(new YoutubeAudioSourceManager());
        PLAYER_MANAGER.registerSourceManager(new BandcampAudioSourceManager());
        PLAYER_MANAGER.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        PLAYER_MANAGER.registerSourceManager(new TwitchStreamAudioSourceManager());
        PLAYER_MANAGER.registerSourceManager(new VimeoAudioSourceManager());
        PLAYER_MANAGER.registerSourceManager(new HttpAudioSourceManager());
    }

    /**
     * @param player the lavalink player that holds the track with data
     * @param message the Base64 audio track
     * @return the AudioTrack with the user data stored in the player
     * @throws IOException if there is an IO problem
     */
    public static AudioTrack toAudioTrackWithData(LavalinkPlayer player, String message) throws IOException{
        AudioTrack storedTrack = player.getPlayingTrack();
        AudioTrack messageTrack = toAudioTrack(player.getLink().getLavalink().getAudioPlayerManager(), message);

        if (storedTrack != null && storedTrack.getUserData() != null) {
            messageTrack.setUserData(storedTrack.getUserData());
        }

        return messageTrack;
    }

    /**
     *
     * @param message the Base64 audio track
     * @return the AudioTrack
     * @throws IOException if there is an IO problem
     */
    @Deprecated
    public static AudioTrack toAudioTrack(String message) throws IOException {
        return toAudioTrack(Base64.getDecoder().decode(message));
    }

    /**
     * @param message the unencoded audio track
     * @return the AudioTrack
     * @throws IOException if there is an IO problem
     */
    @Deprecated
    @SuppressWarnings("WeakerAccess")
    public static AudioTrack toAudioTrack(byte[] message) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(message);
        return PLAYER_MANAGER.decodeTrack(new MessageInput(bais)).decodedTrack;
    }

    /**
     * @param track the track to serialize
     * @return the serialized track a Base64 string
     * @throws IOException if there is an IO problem
     */
    @Deprecated
    public static String toMessage(AudioTrack track) throws IOException {
        return Base64.getEncoder().encodeToString(toBinary(track));
    }

    /**
     * @param track the track to serialize
     * @return the serialized track as binary
     * @throws IOException if there is an IO problem
     */
    @SuppressWarnings("WeakerAccess")
    @Deprecated
    public static byte[] toBinary(AudioTrack track) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PLAYER_MANAGER.encodeTrack(new MessageOutput(baos), track);
        return baos.toByteArray();
    }

    /**
     * @param playerManager AudioPlayerManager to decode the track
     * @param message the Base64 audio track
     * @return the AudioTrack
     * @throws IOException if there is an IO problem
     */
    public static AudioTrack toAudioTrack(AudioPlayerManager playerManager, String message) throws IOException {
        return toAudioTrack(playerManager, Base64.getDecoder().decode(message));
    }

    /**
     * @param playerManager AudioPlayerManager to decode the track
     * @param message the unencoded audio track
     * @return the AudioTrack
     * @throws IOException if there is an IO problem
     */
    @SuppressWarnings("WeakerAccess")
    public static AudioTrack toAudioTrack(AudioPlayerManager playerManager, byte[] message) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(message);
        return playerManager.decodeTrack(new MessageInput(bais)).decodedTrack;
    }

    /**
     * @param playerManager AudioPlayerManager to encode the track
     * @param track the track to serialize
     * @return the serialized track a Base64 string
     * @throws IOException if there is an IO problem
     */
    public static String toMessage(AudioPlayerManager playerManager, AudioTrack track) throws IOException {
        return Base64.getEncoder().encodeToString(toBinary(playerManager, track));
    }

    /**
     * @param playerManager AudioPlayerManager to encode the track
     * @param track the track to serialize
     * @return the serialized track as binary
     * @throws IOException if there is an IO problem
     */
    @SuppressWarnings("WeakerAccess")
    public static byte[] toBinary(AudioPlayerManager playerManager, AudioTrack track) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        playerManager.encodeTrack(new MessageOutput(baos), track);
        return baos.toByteArray();
    }

    public static int getShardFromSnowflake(String snowflake, int numShards) {
        return (int) ((Long.parseLong(snowflake) >> 22) % numShards);
    }

    @Deprecated
    public static AudioPlayerManager getPlayerManager() {
        return PLAYER_MANAGER;
    }

}
