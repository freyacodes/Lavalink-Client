package lavalink.client.io.jda;

import lavalink.client.io.Link;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.GuildUnavailableException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class JdaLink extends Link {

    private static final Logger log = LoggerFactory.getLogger(JdaLink.class);
    private final JdaLavalink lavalink;

    JdaLink(JdaLavalink lavalink, String guildId) {
        super(lavalink, guildId);
        this.lavalink = lavalink;
    }

    public void connect(VoiceChannel voiceChannel) {
        connect(voiceChannel, true);
    }

    /**
     * Eventually connect to a channel. Takes care of disconnecting from an existing connection
     *
     * @param channel Channel to connect to
     */
    @SuppressWarnings("WeakerAccess")
    void connect(VoiceChannel channel, boolean checkChannel) {
        if (!channel.getGuild().equals(getJda().getGuildById(guild)))
            throw new IllegalArgumentException("The provided VoiceChannel is not a part of the Guild that this AudioManager handles." +
                    "Please provide a VoiceChannel from the proper Guild");
        if (!channel.getGuild().isAvailable())
            throw new GuildUnavailableException("Cannot open an Audio Connection with an unavailable guild. " +
                    "Please wait until this Guild is available to open a connection.");
        final Member self = channel.getGuild().getSelfMember();
        if (!self.hasPermission(channel, Permission.VOICE_CONNECT) && !self.hasPermission(channel, Permission.VOICE_MOVE_OTHERS))
            throw new InsufficientPermissionException(Permission.VOICE_CONNECT);

        //If we are already connected to this VoiceChannel, then do nothing.
        if (checkChannel && channel.equals(channel.getGuild().getSelfMember().getVoiceState().getChannel()))
            return;

        if (channel.getGuild().getSelfMember().getVoiceState().inVoiceChannel()) {
            final int userLimit = channel.getUserLimit(); // userLimit is 0 if no limit is set!
            if (!self.isOwner() && !self.hasPermission(Permission.ADMINISTRATOR)) {
                if (userLimit > 0                                                      // If there is a userlimit
                        && userLimit <= channel.getMembers().size()                    // if that userlimit is reached
                        && !self.hasPermission(channel, Permission.VOICE_MOVE_OTHERS)) // If we don't have voice move others permissions
                    throw new InsufficientPermissionException(Permission.VOICE_MOVE_OTHERS, // then throw exception!
                            "Unable to connect to VoiceChannel due to userlimit! Requires permission VOICE_MOVE_OTHERS to bypass");
            }
        }

        setState(State.CONNECTING);
        queueAudioConnect(channel.getIdLong());
    }

    @SuppressWarnings("WeakerAccess")
    @Nonnull
    public JDA getJda() {
        return lavalink.getJdaFromSnowflake(String.valueOf(guild));
    }

    @Override
    protected void removeConnection() {
        // JDA handles this for us
    }

    @Override
    protected void queueAudioDisconnect() {
        Guild g = getJda().getGuildById(guild);

        if (g != null) {
            getJda().getDirectAudioController().disconnect(g);
        } else {
            log.warn("Attempted to disconnect, but guild {} was not found", guild);
        }
    }

    @Override
    protected void queueAudioConnect(long channelId) {
        VoiceChannel vc = getJda().getVoiceChannelById(channelId);
        if (vc != null) {
            getJda().getDirectAudioController().connect(vc);
        } else {
            log.warn("Attempted to connect, but voice channel {} was not found", channelId);
        }
    }

    /**
     * @return the Guild, or null if it doesn't exist
     */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    public Guild getGuild() {
        return getJda().getGuildById(guild);
    }
}
