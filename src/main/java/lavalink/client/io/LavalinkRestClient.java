package lavalink.client.io;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import edu.umd.cs.findbugs.annotations.NonNull;
import lavalink.client.LavalinkUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public final class LavalinkRestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LavalinkRestClient.class);

    private static final Function<JSONObject, List<AudioTrack>> SEARCH_TRANSFORMER = loadResult -> {
        final List<AudioTrack> tracks = new ArrayList<>();
        final JSONArray trackData = loadResult.getJSONArray("tracks");

        for (final Object track : trackData) {
            try {
                final AudioTrack audioTrack = LavalinkUtil.toAudioTrack(((JSONObject) track).getString("track"));
                tracks.add(audioTrack);
            } catch (final IOException exception) {
                LOGGER.error("Error converting track", exception);
            }
        }

        return tracks;
    };

    private final LavalinkSocket socket;
    private Consumer<HttpClientBuilder> builderConsumer;

    public LavalinkRestClient(final LavalinkSocket socket) {
        this.socket = socket;
    }

    public void setHttpClientBuilder(final Consumer<HttpClientBuilder> clientBuilder) {
        this.builderConsumer = clientBuilder;
    }

    @NonNull
    public CompletableFuture<List<AudioTrack>> getYoutubeSearchResult(final String query) {
        return load("ytsearch:" + query)
                .thenApplyAsync(SEARCH_TRANSFORMER);
    }

    @NonNull
    public CompletableFuture<List<AudioTrack>> getSoundcloudSearchResult(final String query) {
        return load("scsearch:" + query)
                .thenApplyAsync(SEARCH_TRANSFORMER);
    }

    @NonNull
    public CompletableFuture<Void> loadItem(final String identifier, final AudioLoadResultHandler callback) {
        return load(identifier)
                .thenAcceptAsync(consumeCallback(callback));
    }

    private Consumer<JSONObject> consumeCallback(final AudioLoadResultHandler callback) {
        return loadResult -> {
            if (loadResult == null) {
                callback.noMatches();
                return;
            }

            try {
                final String loadType = loadResult.getString("loadType");
                final TrackLoadResultHandler trackLoadResultHandler = new TrackLoadResultHandler(loadResult);

                switch (loadType) {
                    case "TRACK_LOADED":
                        callback.trackLoaded(trackLoadResultHandler.handleTrackLoaded());
                        break;
                    case "PLAYLIST_LOADED":
                        callback.playlistLoaded(trackLoadResultHandler.handlePlaylistLoaded(false));
                        break;
                    case "NO_MATCHES":
                        callback.noMatches();
                        break;
                    case "LOAD_FAILED":
                        callback.loadFailed(trackLoadResultHandler.handleLoadFailed());
                        break;
                    case "SEARCH_RESULT":
                        callback.playlistLoaded(trackLoadResultHandler.handlePlaylistLoaded(true));
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid loadType: " + loadType);
                }
            } catch (final Exception exception) {
                callback.loadFailed(new FriendlyException(exception.getMessage(), FriendlyException.Severity.FAULT, exception));
            }
        };
    }

    private CompletableFuture<JSONObject> load(final String identifier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final String requestURL = buildBaseAddress(socket) + URLEncoder.encode(identifier, "UTF-8");
                return apiGet(requestURL, socket.getPassword());
            } catch (final Throwable exception) {
                exception.printStackTrace();
            }

            return null;
        });
    }

    private String buildBaseAddress(final LavalinkSocket socket) {
        return socket.getRemoteUri().toString()
                .replaceFirst("ws://", "http://")
                .replaceFirst("wss://", "https://")
                .concat("/loadtracks?identifier=");
    }

    private HttpClient buildClient() {
        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if (builderConsumer == null) return httpClientBuilder.build();

        builderConsumer.accept(httpClientBuilder);
        return httpClientBuilder.build();
    }

    private JSONObject apiGet(final String url, final String auth) throws IOException {
        final HttpGet request = new HttpGet(url);
        request.addHeader(HttpHeaders.AUTHORIZATION, auth);

        final HttpClient httpClient = buildClient();
        final HttpResponse httpResponse = httpClient.execute(request);
        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode != 200) throw new IOException("Invalid API Request Status Code: " + statusCode);

        final HttpEntity entity = httpResponse.getEntity();
        if (entity == null) throw new IOException("Invalid API Response: No Content");

        final String response = EntityUtils.toString(entity);
        return new JSONObject(response);
    }

    private static final class TrackLoadResultHandler {

        private final JSONObject loadResult;

        private TrackLoadResultHandler(JSONObject loadResult) {
            this.loadResult = loadResult;
        }

        private AudioTrack handleTrackLoaded() throws IOException {
            final JSONArray trackDataSingle = loadResult.getJSONArray("tracks");
            final JSONObject trackObject = trackDataSingle.getJSONObject(0);
            final String singleTrackBase64 = trackObject.getString("track");

            return LavalinkUtil.toAudioTrack(singleTrackBase64);
        }

        private AudioPlaylist handlePlaylistLoaded(boolean isSearchResult) throws Exception {
            final JSONArray trackData = loadResult.getJSONArray("tracks");
            final List<AudioTrack> tracks = new ArrayList<>();

            for (final Object track : trackData) {
                final String trackBase64 = ((JSONObject) track).getString("track");
                final AudioTrack audioTrack = LavalinkUtil.toAudioTrack(trackBase64);

                tracks.add(audioTrack);
            }

            if (isSearchResult) {
                if (tracks.size() == 0) {
                    throw new FriendlyException(
                            "No search results found",
                            FriendlyException.Severity.COMMON,
                            new IllegalStateException("No results")
                    );
                }

                return new BasicAudioPlaylist("Search results for: ", tracks, tracks.get(0), true);
            }

            final JSONObject playlistInfo = loadResult.getJSONObject("playlistInfo");
            final int selectedTrackID = playlistInfo.getInt("selectedTrack");
            final AudioTrack selectedTrack;

            if (selectedTrackID < tracks.size() && selectedTrackID >= 0) {
                selectedTrack = tracks.get(selectedTrackID);
            } else {
                if (tracks.size() == 0) {
                    throw new FriendlyException(
                            "Playlist is empty",
                            FriendlyException.Severity.SUSPICIOUS,
                            new IllegalStateException("Empty playlist")
                    );
                }

                selectedTrack = tracks.get(0);
            }

            final String playlistName = playlistInfo.getString("name");

            return new BasicAudioPlaylist(playlistName, tracks, selectedTrack, false);
        }

        private FriendlyException handleLoadFailed() {
            final JSONObject exception = loadResult.getJSONObject("exception");
            final String message = exception.getString("message");
            final FriendlyException.Severity severity = FriendlyException.Severity.valueOf(exception.getString("severity"));

            return new FriendlyException(message, severity, new Throwable());
        }
    }
}
