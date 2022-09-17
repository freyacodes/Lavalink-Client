package lavalink.client.io.jda;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import lavalink.client.io.GuildUnavailableException;
import lavalink.client.io.Link;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdaLink extends Link {

    private static final Logger log = LoggerFactory.getLogger(JdaLink.class);
    private final JdaLavalink lavalink;

    JdaLink(JdaLavalink lavalink, String guildId) {
        super(lavalink, guildId);
        this.lavalink = lavalink;
    }

    public void connect(@NonNull AudioChannel audioChannel) {
        connect(audioChannel, true);
    }

    /**
     * Eventually connect to a channel. Takes care of disconnecting from an existing connection
     *
     * @param channel Channel to connect to
     */
    @SuppressWarnings("WeakerAccess")
    void connect(@NonNull AudioChannel channel, boolean checkChannel) {
        if (!channel.getGuild().equals(getJda().getGuildById(guild)))
            throw new IllegalArgumentException("The provided AudioChannel is not a part of the Guild that this AudioManager handles." +
                    "Please provide an AudioChannel from the proper Guild");
        if (channel.getJDA().isUnavailable(channel.getGuild().getIdLong()))
            throw new GuildUnavailableException("Cannot open an Audio Connection with an unavailable guild. " +
                    "Please wait until this Guild is available to open a connection.");
        final Member self = channel.getGuild().getSelfMember();
        if (!self.hasPermission(channel, Permission.VOICE_CONNECT) && !self.hasPermission(channel, Permission.VOICE_MOVE_OTHERS))
            throw new InsufficientPermissionException(channel, Permission.VOICE_CONNECT);

        //If we are already connected to this VoiceChannel, then do nothing.
        GuildVoiceState voiceState = channel.getGuild().getSelfMember().getVoiceState();
        if (voiceState == null) return;

        if (checkChannel && channel.equals(voiceState.getChannel()))
            return;

        if (voiceState.inAudioChannel()) {
            int userLimit = 0; // userLimit is 0 if no limit is set!

            if (channel instanceof VoiceChannel) {
                VoiceChannel vc = (VoiceChannel) channel;
                userLimit = vc.getUserLimit();
            }

            if (!self.isOwner() && !self.hasPermission(Permission.ADMINISTRATOR)) {
                if (userLimit > 0                                                      // If there is a userlimit
                        && userLimit <= channel.getMembers().size()                    // if that userlimit is reached
                        && !self.hasPermission(channel, Permission.VOICE_MOVE_OTHERS)) // If we don't have voice move others permissions
                    throw new InsufficientPermissionException(channel, Permission.VOICE_MOVE_OTHERS, // then throw exception!
                            "Unable to connect to VoiceChannel due to userlimit! Requires permission VOICE_MOVE_OTHERS to bypass");
            }
        }

        setState(State.CONNECTING);
        queueAudioConnect(channel.getIdLong());
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
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
        AudioChannel channel = getJda().getChannelById(AudioChannel.class, channelId);
        if (channel != null) {
            getJda().getDirectAudioController().connect(channel);
        } else {
            log.warn("Attempted to connect, but AudioChannel {} was not found", channelId);
        }
    }

    /**
     * @return the Guild, or null if it doesn't exist
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    @Nullable
    public Guild getGuild() {
        return getJda().getGuildById(guild);
    }
}
