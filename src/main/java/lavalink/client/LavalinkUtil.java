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
     *
     * @param player the lavalink player that holds the track with data
     * @param message the Base64 audio track
     * @return the AudioTrack with the user data stored in the player
     * @throws IOException if there is an IO problem
     */
    public static AudioTrack toAudioTrackWithData(LavalinkPlayer player, String message) throws IOException{
        AudioTrack storedTrack = player.getPlayingTrack();
        AudioTrack messageTrack = toAudioTrack(message);

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
    public static AudioTrack toAudioTrack(String message) throws IOException {
        return toAudioTrack(Base64.getDecoder().decode(message));
    }

    /**
     * @param message the unencoded audio track
     * @return the AudioTrack
     * @throws IOException if there is an IO problem
     */
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
    public static String toMessage(AudioTrack track) throws IOException {
        return Base64.getEncoder().encodeToString(toBinary(track));
    }

    /**
     * @param track the track to serialize
     * @return the serialized track as binary
     * @throws IOException if there is an IO problem
     */
    @SuppressWarnings("WeakerAccess")
    public static byte[] toBinary(AudioTrack track) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PLAYER_MANAGER.encodeTrack(new MessageOutput(baos), track);
        return baos.toByteArray();
    }

    public static int getShardFromSnowflake(String snowflake, int numShards) {
        return (int) ((Long.parseLong(snowflake) >> 22) % numShards);
    }

    public static AudioPlayerManager getPlayerManager() {
        return PLAYER_MANAGER;
    }

}
