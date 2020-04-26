package lavalink.client.io.d4j;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceServerUpdateEvent;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.lifecycle.ReconnectEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.Snowflake;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import lavalink.client.io.Lavalink;
import lavalink.client.io.Link;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public class D4JLavalink extends Lavalink<D4JLink> {

    private static final Logger log = LoggerFactory.getLogger(D4JLavalink.class);

    private final GatewayDiscordClient client;
    private boolean autoReconnect = true;

    public D4JLavalink(final String userId, final int numShards, final GatewayDiscordClient client) {
        super(userId, numShards);
        this.client = client;
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    public GatewayDiscordClient getGateway() {
        return client;
    }

    @NonNull
    public D4JLink getLink(Guild guild) {
        return getLink(guild.getId().asString());
    }

    @Nullable
    public D4JLink getExistingLink(Guild guild) {
        return getExistingLink(guild.getId().asString());
    }

    public Mono<Void> registerEvents() {
        final Mono<Void> reconnectEvent = client.on(ReconnectEvent.class)
                .filter(event -> isAutoReconnect())
                .flatMap(event -> Flux.fromIterable(getLinks()))
                .filter(entry -> entry.getLastChannel() != null)
                .flatMap(entry -> Mono.zip(Mono.just(entry), client.getGuildById(Snowflake.of(entry.getGuildId()))))
                .flatMap(tuple -> Mono.zip(Mono.just(tuple.getT1()),
                        tuple.getT2().getChannelById(Snowflake.of(tuple.getT1().getLastChannel()))
                                .cast(VoiceChannel.class)))
                .doOnNext(tuple -> tuple.getT1().connect(tuple.getT2(), false))
                .then();

        final Mono<Void> guildDeleteEvent = client.on(GuildDeleteEvent.class)
                .map(event -> getExistingLink(event.getGuildId().asString()))
                .doOnNext(D4JLink::removeConnection)
                .then();

        final Mono<Void> channelDeleteEvent = client.on(VoiceChannelDeleteEvent.class)
                .flatMap(event -> Mono.zip(Mono.just(event),
                        Mono.fromCallable(() -> getExistingLink(event.getChannel().getGuildId().asString()))))
                .filter(tuple -> tuple.getT1().getChannel().getId().equals(Snowflake.of(tuple.getT2().getLastChannel())))
                .map(Tuple2::getT2)
                .doOnNext(D4JLink::removeConnection)
                .then();

        final Mono<Void> voiceServerUpdateEvent = client.on(VoiceServerUpdateEvent.class)
                .flatMap(event -> Mono.zip(Mono.just(event), event.getGuild()))
                .flatMap(tuple -> Mono.zip(Mono.just(tuple.getT1()), tuple.getT2().getSelfMember()))
                .flatMap(tuple -> Mono.zip(Mono.just(tuple.getT1()), tuple.getT2().getVoiceState()))
                .flatMap(tuple -> Mono.zip(Mono.just(getLink(tuple.getT1().getGuildId().asString())),
                        Mono.fromCallable(() -> new JSONObject()
                                .put("token", tuple.getT1().getToken())
                                .put("guild_id", tuple.getT1().getGuildId().asString())
                                .put("endpoint", tuple.getT1().getEndpoint())),
                        Mono.just(tuple.getT2().getSessionId())))
                .doOnNext(triple -> triple.getT1().onVoiceServerUpdate(triple.getT2(), triple.getT3()))
                .then();

        final Mono<Void> voiceStateUpdateEvent = client.on(VoiceStateUpdateEvent.class)
                .flatMap(event -> Mono.zip(
                        event.getCurrent().getChannel().map(Possible::of).defaultIfEmpty(Possible.absent()),
                        Mono.fromCallable(() -> getLink(event.getCurrent().getGuildId().asString()))))
                .doOnNext(tuple -> {
                    if (tuple.getT1().isAbsent()) {
                        if (tuple.getT2().getState() != Link.State.DESTROYED) {
                            tuple.getT2().onDisconnected();
                        }
                    } else {
                        tuple.getT2().setChannel(tuple.getT1().get().getId().asString());
                    }
                })
                .then();

        return Mono.when(reconnectEvent, guildDeleteEvent, channelDeleteEvent, voiceServerUpdateEvent,
                voiceStateUpdateEvent);
    }

    @Override
    protected D4JLink buildNewLink(String guildId) {
        return new D4JLink(this, guildId);
    }
}
