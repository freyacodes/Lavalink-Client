package lavalink.client.io.jda;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import lavalink.client.LavalinkUtil;
import lavalink.client.io.Lavalink;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.session.SessionRecreateEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class JdaLavalink extends Lavalink<JdaLink> implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(JdaLavalink.class);

    /** JDA provider may be set at a later time */
    @Nullable
    private Function<Integer, JDA> jdaProvider;
    private boolean autoReconnect = true;
    private final JDAVoiceInterceptor voiceInterceptor;

    public JdaLavalink(String userId, int numShards, @Nullable Function<Integer, JDA> jdaProvider) {
        super(userId, numShards);
        this.jdaProvider = jdaProvider;
        this.voiceInterceptor = new JDAVoiceInterceptor(this);
    }

    /**
     * Creates a Lavalink instance.
     * N.B: You must set the user ID before adding a node
     */
    public JdaLavalink(int numShards, @Nullable Function<Integer, JDA> jdaProvider) {
        this(null, numShards, jdaProvider);
    }

    @SuppressWarnings("unused")
    public JdaLavalink(int numShards) {
        this(numShards, null);
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

    @SuppressWarnings({"WeakerAccess", "unused"})
    @Nullable
    public JdaLink getExistingLink(Guild guild) {
        return getExistingLink(guild.getId());
    }

    /**
     * Returns the JDA instance with the {@code shardId shard ID}
     *
     * @param shardId the ID of the shard
     * @return the JDA instance with the specified shard ID
     *
     * @throws IllegalStateException if the JDA provider has not been initialised
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    @NonNull
    public JDA getJda(int shardId) {
        if (jdaProvider == null) throw new IllegalStateException("JDAProvider is not initialised!");

        JDA result = jdaProvider.apply(shardId);
        if (result == null) throw new IllegalStateException("JDAProvider returned null for shard " + shardId);

        return jdaProvider.apply(shardId);
    }

    /**
     * Returns the JDA instance with the {@code snowflake snowflake}
     *
     * @param snowflake the snowflake of the shard
     * @return the JDA instance with the specified snowflake
     *
     * @throws IllegalStateException if the JDA provider has not been initialised
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public JDA getJdaFromSnowflake(String snowflake) {
        return getJda(LavalinkUtil.getShardFromSnowflake(snowflake, numShards));
    }

    @SuppressWarnings("unused")
    public void setJdaProvider(@Nullable Function<Integer, JDA> jdaProvider) {
        this.jdaProvider = jdaProvider;
    }

    @SuppressWarnings("unused")
    @NonNull
    public JDAVoiceInterceptor getVoiceInterceptor() {
        return voiceInterceptor;
    }

    @Override
    public void onEvent(@NonNull GenericEvent event) {
        if (event instanceof SessionRecreateEvent) {
            if (autoReconnect) {
                getLinksMap().forEach((guildId, link) -> {
                    try {
                        //Note: We also ensure that the link belongs to the JDA object
                        if (link.getLastChannel() != null
                                && event.getJDA().getGuildById(guildId) != null) {
                            link.connect(event.getJDA().getChannelById(AudioChannel.class, link.getLastChannel()), false);
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
        } else if (event instanceof ChannelDeleteEvent) {
            ChannelDeleteEvent e = (ChannelDeleteEvent) event;
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
