package lavalink.client.io;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import lavalink.client.LavalinkUtil;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class LavalinkRestClient {

    private static final Logger log = Loggers.getLogger(LavalinkRestClient.class);

    private static final Function<String, Mono<AudioTrack>> CONVERT_AUDIO = s -> Mono.fromCallable(() -> LavalinkUtil.toAudioTrack(s));

    private static final Function<JSONObject, Flux<AudioTrack>> SEARCH_TRANSFORMER =
            loadResult -> Flux.fromIterable(loadResult.getJSONArray("tracks"))
                    .ofType(JSONObject.class)
                    .map(json -> json.getString("track"))
                    .flatMap(CONVERT_AUDIO)
                    .onErrorContinue((throwable, o) -> log.error("Error converting track {}", o, throwable));

    private final HttpClient httpClient;
    private final LavalinkSocket node;

    public LavalinkRestClient(final LavalinkSocket node) {
        this.httpClient = HttpClient.create();
        this.node = node;
    }

    public Flux<AudioTrack> getYoutubeSearchResult(final String query) {
        return load("ytsearch:" + query)
                .flatMapMany(SEARCH_TRANSFORMER);
    }

    private Mono<JSONObject> load(final String identifier) {
        return Mono.fromCallable(() -> getApiBaseUrl() + "/loadtracks?identifier=" + URLEncoder.encode(identifier,
                "UTF-8")).flatMap(this::apiCall);
    }

    private String getApiBaseUrl() {
        return node.getRemoteUri().toString().replaceFirst("[wW][sS]{1,2}[:][/]{2}", "http://");
    }

    private Mono<JSONObject> apiCall(final String url) {
        return httpClient
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, node.getPassword()))
                .get()
                .uri(url)
                .responseSingle((httpClientResponse, byteBufMono) -> Mono.zip(Mono.just(httpClientResponse),
                        byteBufMono.asString(StandardCharsets.UTF_8)))
                .flatMap(tuple -> {
                    final int code = tuple.getT1().status().code();
                    if (code != 200) {
                        return Mono.error(new IllegalStateException("Illegal HTTP Status Code: " + code + " | " + tuple.getT1().status().reasonPhrase()));
                    } else {
                        return Mono.just(tuple.getT2());
                    }
                })
                .map(JSONObject::new);
    }

    public Flux<AudioTrack> getSoundCloudSearchResult(final String query) {
        return load("scsearch:" + query)
                .flatMapMany(SEARCH_TRANSFORMER);
    }

    public Mono<AudioItem> loadItem(final String identifier) {
        return load(identifier)
                .flatMap(loadResult -> {
                    try {
                        final String loadType = loadResult.getString("loadType");
                        switch (loadType) {
                            case "TRACK_LOADED":
                                final JSONArray trackDataSingle = loadResult.getJSONArray("tracks");
                                final JSONObject trackObject = trackDataSingle.getJSONObject(0);
                                final String singleTrackBase64 = trackObject.getString("track");
                                return Mono.fromCallable(() -> LavalinkUtil.toAudioTrack(singleTrackBase64));
                            case "PLAYLIST_LOADED":
                                final JSONObject playlistInfo = loadResult.getJSONObject("playlistInfo");
                                final int selectedTrackId = playlistInfo.getInt("selectedTrack");
                                final String playlistName = playlistInfo.getString("name");
                                final Flux<AudioTrack> tracks = Flux.fromIterable(loadResult.getJSONArray(
                                        "tracks"))
                                        .ofType(JSONObject.class)
                                        .map(json -> json.getString("track"))
                                        .flatMap(CONVERT_AUDIO)
                                        .cache();
                                final Mono<AudioTrack> selectedTrack = tracks.count()
                                        .filter(count -> count > selectedTrackId && selectedTrackId >= 0)
                                        .flatMap(count -> tracks.elementAt(selectedTrackId))
                                        .switchIfEmpty(tracks.count()
                                                .filter(count -> count > 0)
                                                .switchIfEmpty(Mono.error(new IllegalStateException("Playlist is " +
                                                        "empty")))
                                                .then(tracks.next()));
                                return Mono.zip(tracks.collectList(), selectedTrack)
                                        .map(tuple -> new BasicAudioPlaylist(playlistName, tuple.getT1(),
                                                tuple.getT2(), true));
                            case "NO_MATCHES":
                                return Mono.just(AudioReference.NO_TRACK);
                            case "LOAD_FAILED":
                                final JSONObject exception = loadResult.getJSONObject("exception");
                                final String message = exception.getString("message");
                                final FriendlyException.Severity severity =
                                        FriendlyException.Severity.valueOf(exception.getString("severity"));
                                final FriendlyException friendlyException = new FriendlyException(message, severity,
                                        new Throwable());
                                return Mono.error(friendlyException);
                            default:
                                return Mono.error(new IllegalArgumentException("Invalid loadType: " + loadType));
                        }
                    } catch (final Exception ex) {
                        return Mono.error(ex);
                    }
                });
    }

}
