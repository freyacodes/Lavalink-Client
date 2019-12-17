package lavalink.client.io.jda;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import lavalink.client.LavalinkUtil;
import lavalink.client.io.Lavalink;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class JdaLavalink extends Lavalink<JdaLink> implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(JdaLavalink.class);
    private final Function<Integer, JDA> jdaProvider;
    private boolean autoReconnect = true;
    private final JDAVoiceInterceptor voiceInterceptor;

    public JdaLavalink(String userId, int numShards, Function<Integer, JDA> jdaProvider) {
        super(userId, numShards);
        this.jdaProvider = jdaProvider;
        this.voiceInterceptor = new JDAVoiceInterceptor(this);
    }

    @SuppressWarnings("unused")
    public boolean getAutoReconnect() {
        return autoReconnect;
    }

    @SuppressWarnings("unused")
    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public JdaLink getLink(Guild guild) {
        return getLink(guild.getId());
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    public JdaLink getExistingLink(Guild guild) {
        return getExistingLink(guild.getId());
    }

    @SuppressWarnings({"WeakerAccess", "unused"})
    @NonNull
    public JDA getJda(int shardId) {
        return jdaProvider.apply(shardId);
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public JDA getJdaFromSnowflake(String snowflake) {
        return jdaProvider.apply(LavalinkUtil.getShardFromSnowflake(snowflake, numShards));
    }

    public JDAVoiceInterceptor getVoiceInterceptor() {
        return voiceInterceptor;
    }

    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof ReconnectedEvent) {
            if (autoReconnect) {
                getLinksMap().forEach((guildId, link) -> {
                    try {
                        //Note: We also ensure that the link belongs to the JDA object
                        if (link.getLastChannel() != null
                                && event.getJDA().getGuildById(guildId) != null) {
                            link.connect(event.getJDA().getVoiceChannelById(link.getLastChannel()), false);
                        }
                    } catch (Exception e) {
                        log.error("Caught exception while trying to reconnect link " + link, e);
                    }
                });
            }
        } else if (event instanceof GuildLeaveEvent) {
            JdaLink link = getLinksMap().get(((GuildLeaveEvent) event).getGuild().getId());
            if (link == null) return;

            link.removeConnection();
        } else if (event instanceof VoiceChannelDeleteEvent) {
            VoiceChannelDeleteEvent e = (VoiceChannelDeleteEvent) event;
            JdaLink link = getLinksMap().get(e.getGuild().getId());
            if (link == null || !e.getChannel().getId().equals(link.getLastChannel())) return;

            link.removeConnection();
        }
    }

    @Override
    protected JdaLink buildNewLink(String guildId) {
        return new JdaLink(this, guildId);
    }
}
