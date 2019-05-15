/*
 * Written by TheMulti0, 2019
 * https://github.com/TheMulti0
 */

package lavalink.client.mock;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.io.Lavalink;
import lavalink.client.io.Link;
import lavalink.client.io.mock.MockLavalink;
import lavalink.client.player.LavalinkPlayer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MockLavalinkTest {

    private static final Logger log = LoggerFactory.getLogger(MockLavalinkTest.class);
    private final YoutubeAudioSourceManager youtube = new YoutubeAudioSourceManager();
    private final DefaultAudioPlayerManager playerManager = new DefaultAudioPlayerManager();

    @Test
    public void play() throws InterruptedException {
        Lavalink lavalink = new MockLavalink();
        Link link = lavalink.getLink("435874356896543567");
        LavalinkPlayer player = link.getPlayer();
        player.playTrack(loadAudioTracks("Hi").get(0));

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                AudioTrack playingTrack = player.getPlayingTrack();
                if (playingTrack == null) {
                    timer.cancel();
                } else {
                    String format = String.format("Track %s is playing, %d / %d", playingTrack.getInfo().title, player.getTrackPosition(), playingTrack.getDuration());
                    log.info(format);
                }
            }
        }, 0, 100);
        Thread.sleep(10000000);
    }

    private List<AudioTrack> loadAudioTracks(String identifier) {
        playerManager.registerSourceManager(youtube);
        AudioItem audioItem = youtube.loadItem(playerManager, new AudioReference("ytsearch:" + identifier, null));
        if (audioItem instanceof AudioPlaylist){
            return ((AudioPlaylist) audioItem).getTracks();
        }
        if (audioItem instanceof AudioTrack){
            return Collections.singletonList((AudioTrack) audioItem);
        }
        return null;
    }

}
