package lavalink.client.io.d4j;

import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.discordjson.json.gateway.VoiceStateUpdate;
import discord4j.gateway.GatewayClientGroup;
import discord4j.gateway.json.ShardGatewayPayload;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import discord4j.rest.util.Snowflake;
import lavalink.client.io.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class D4JLink extends Link {

    private static final Logger log = LoggerFactory.getLogger(D4JLink.class);
    private final D4JLavalink lavalink;

    D4JLink(final D4JLavalink lavalink, final String guildId) {
        super(lavalink, guildId);
        this.lavalink = lavalink;
    }

    public void connect(VoiceChannel voiceChannel) {
        connect(voiceChannel, true);
    }

    void connect(VoiceChannel channel, boolean checkChannel) {
        if (!channel.getGuildId().equals(Snowflake.of(getGuildId()))) {
            throw new IllegalArgumentException("The provided VoiceChannel is not a part of the Guild that this " +
                    "AudioManager handles." +
                    "Please provide a VoiceChannel from the proper Guild");
        }

        final Guild guild = channel.getGuild().block();

        if (guild == null) {
            return;
        }

        if (guild.isUnavailable()) {
            throw new IllegalStateException("Cannot open an Audio Connection with an unavailable guild. " +
                    "Please wait until this Guild is available to open a connection.");
        }

        final Member self = guild.getSelfMember().block();

        if (self == null) {
            return;
        }

        final PermissionSet permissions = channel.getEffectivePermissions(self.getId()).block();

        if (!permissions.contains(Permission.CONNECT) && !permissions.contains(Permission.MOVE_MEMBERS)) {
            throw new IllegalStateException("Missing permission: " + Permission.CONNECT);
        }

        final Optional<Snowflake> selfChannel = self.getVoiceState().map(VoiceState::getChannelId).block();

        //If we are already connected to this VoiceChannel, then do nothing.
        if (checkChannel && selfChannel != null && selfChannel.isPresent() && selfChannel.get().equals(channel.getId())) {
            return;
        }

        if (selfChannel != null && selfChannel.isPresent()) {
            final int userLimit = channel.getUserLimit(); // userLimit is 0 if no limit is set!
            /*if (!self.isOwner() && !self.hasPermission(Permission.ADMINISTRATOR)) {
                if (userLimit > 0                                                      // If there is a userlimit
                        && userLimit <= channel.getMembers().size()                    // if that userlimit is reached
                        && !self.hasPermission(channel, Permission.VOICE_MOVE_OTHERS)) // If we don't have voice move
                // others permissions
                {
                    throw new InsufficientPermissionException(channel, Permission.VOICE_MOVE_OTHERS, // then throw
                            // exception!
                            "Unable to connect to VoiceChannel due to userlimit! Requires permission " +
                                    "VOICE_MOVE_OTHERS to bypass");
                }
            }
             */
        }

        setState(State.CONNECTING);
        queueAudioConnect(channel.getId().asLong());
    }

    @Override
    protected void removeConnection() {
    }

    @Override
    protected void queueAudioDisconnect() {
        final GatewayClientGroup clientGroup = lavalink.getGateway().getGatewayClientGroup();
        final long guildId = getGuildIdLong();
        clientGroup.unicast(ShardGatewayPayload.voiceStateUpdate(VoiceStateUpdate.builder()
                        .guildId(getGuildId())
                        .selfDeaf(false)
                        .selfMute(false)
                        .build(),
                (int) ((guildId >> 22) % clientGroup.getShardCount())))
                .subscribe();
    }

    @Override
    protected void queueAudioConnect(long channelId) {
        lavalink.getGateway().getChannelById(Snowflake.of(channelId))
                .ofType(VoiceChannel.class)
                .flatMap(channel -> channel.getClient().getGatewayClientGroup().unicast(ShardGatewayPayload.voiceStateUpdate(
                        VoiceStateUpdate.builder()
                                .guildId(channel.getGuildId().asString())
                                .channelId(channel.getId().asString())
                                .selfDeaf(false)
                                .selfMute(false)
                                .build(),
                        (int) ((channel.getId().asLong() >> 22) % channel.getClient().getGatewayClientGroup().getShardCount())
                )))
                .subscribe();
    }

}
