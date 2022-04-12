package lavalink.client.io.jda;

import lavalink.client.io.Link;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * You have to set this class on the JDABuilder or DefaultShardManagerBuilder
 */
public class JDAVoiceInterceptor implements VoiceDispatchInterceptor {

    private final JdaLavalink lavalink;

    public JDAVoiceInterceptor(JdaLavalink lavalink) {
        this.lavalink = lavalink;
    }

    @Override
    public void onVoiceServerUpdate(@Nonnull VoiceServerUpdate update) {
        JSONObject content = new JSONObject(update.toData().getObject("d").toMap());

        // Get session
        Guild guild = update.getGuild();

        lavalink.getLink(guild).onVoiceServerUpdate(content, guild.getSelfMember().getVoiceState().getSessionId());
    }

    @Override
    public boolean onVoiceStateUpdate(@Nonnull VoiceStateUpdate update) {
        AudioChannel channel = update.getChannel();
        JdaLink link = lavalink.getLink(update.getGuildId());

        if (channel == null) {
            // Null channel means disconnected
            if (link.getState() != Link.State.DESTROYED) {
                link.onDisconnected();
            }
        } else {
            link.setChannel(channel.getId()); // Change expected channel
        }

        return link.getState() == Link.State.CONNECTED;
    }
}
