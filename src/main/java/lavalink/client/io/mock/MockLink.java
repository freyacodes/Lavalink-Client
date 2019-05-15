/*
 * Written by TheMulti0, 2019
 * https://github.com/TheMulti0
 */

package lavalink.client.io.mock;

import edu.umd.cs.findbugs.annotations.Nullable;
import lavalink.client.io.Lavalink;
import lavalink.client.io.LavalinkSocket;
import lavalink.client.io.Link;
import org.json.JSONObject;
import org.mockito.Mockito;

public class MockLink extends Link {
    protected MockLink(Lavalink lavalink, String guildId) {
        super(lavalink, guildId);
    }

    @Override
    protected void removeConnection() {
    }

    @Override
    protected void queueAudioDisconnect() {
    }

    @Override
    protected void queueAudioConnect(long channelId) {
    }

    // Custom implementations

    @Override
    public void disconnect() {

    }

    @Override
    public void changeNode(LavalinkSocket newNode) {
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void destroy() {
    }

    @Nullable
    @Override
    public LavalinkSocket getNode(boolean selectIfAbsent) {
        return Mockito.mock(LavalinkSocket.class);
    }

    @Override
    public void onVoiceServerUpdate(JSONObject json, String sessionId) {
    }

    @Override
    public void onVoiceWebSocketClosed(int code, String reason, boolean byRemote) {
    }
}
