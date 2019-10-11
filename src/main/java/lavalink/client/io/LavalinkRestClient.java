package lavalink.client.io;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import lavalink.client.LavalinkUtil;
import lavalink.client.io.LavalinkSocket;
import lavalink.client.io.jda.JdaLink;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class LavalinkRestClient {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(new DaemonThreadFactory("Lavalink-RestExecutor"));
    private static final Consumer<IOException> DEFAULT_ERROR_CONSUMER = Throwable::printStackTrace;

    private final JdaLink link;
    private Consumer<HttpClientBuilder> builderConsumer;

    public LavalinkRestClient(final JdaLink link) {
        this.link = link;
    }

    private LavalinkSocket getSocket() {
        return link.getNode(true);
    }

    private String buildBaseAddress(final LavalinkSocket socket) {
        return socket.getRemoteUri().toString().replace("ws://", "http://") + "/loadtracks?identifier=";
    }

    private HttpClient buildClient() {
        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if (builderConsumer == null)
            return httpClientBuilder.build();
        builderConsumer.accept(httpClientBuilder);
        return httpClientBuilder.build();
    }

    private JSONObject apiGet(final String url, final String auth) throws IOException {
        final HttpGet request = new HttpGet(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, auth);
        final HttpClient httpClient = buildClient();
        final HttpResponse httpResponse = httpClient.execute(request);
        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode != 200)
            throw new IOException("Invalid API Request Status Code: " + statusCode);
        final HttpEntity entity = httpResponse.getEntity();
        if (entity == null)
            throw new IOException("Invalid API Response: No Content");
        final String response = EntityUtils.toString(entity);
        return new JSONObject(response);
    }

    public void setHttpClientBuilder(final Consumer<HttpClientBuilder> clientBuilder) {
        this.builderConsumer = clientBuilder;
    }

    public void getYoutubeSearchResults(final String query, final Consumer<List<AudioTrack>> callback) {
        getYoutubeSearchResults(query, callback, DEFAULT_ERROR_CONSUMER);
    }

    public void getYoutubeSearchResults(final String query, final Consumer<List<AudioTrack>> callback, final Consumer<IOException> errorCallback) {
        EXECUTOR_SERVICE.submit(() -> {
            try {
                final List<AudioTrack> tracks = getYoutubeSearchResultsSync(query);
                callback.accept(tracks);
            } catch (final IOException ex) {
                errorCallback.accept(ex);
            }
        });
    }

    public List<AudioTrack> getYoutubeSearchResultsSync(final String query) throws IOException {
        final LavalinkSocket socket = getSocket();
        final String requestUrl = buildBaseAddress(socket) + URLEncoder.encode("ytsearch: " + query, StandardCharsets.UTF_8.name());
        final List<AudioTrack> tracks = new ArrayList<>();
        final JSONObject response = apiGet(requestUrl, socket.getPassword());
        final JSONArray trackData = response.getJSONArray("tracks");
        for (final Object track : trackData) {
            final AudioTrack audioTrack = LavalinkUtil.toAudioTrack(((JSONObject) track).getString("track"));
            tracks.add(audioTrack);
        }
        return tracks;
    }

    public void loadItem(final String url, final AudioLoadResultHandler callback) {
        loadItem(url, callback, DEFAULT_ERROR_CONSUMER);
    }

    public void loadItem(final String url, final AudioLoadResultHandler callback, final Consumer<IOException> errorCallback) {
        EXECUTOR_SERVICE.submit(() -> {
            try {
                loadItemSync(url, callback);
            } catch (final IOException ex) {
                errorCallback.accept(ex);
            }

        });
    }

    public void loadItemSync(final String url, final AudioLoadResultHandler callback) throws IOException {
        final LavalinkSocket socket = getSocket();
        final String requestUrl = buildBaseAddress(socket) + URLEncoder.encode(url, StandardCharsets.UTF_8.name());
        final JSONObject response = apiGet(requestUrl, socket.getPassword());
        final String loadType = response.getString("loadType");
        switch (loadType) {
            case "TRACK_LOADED":
                final JSONArray trackDataSingle = response.getJSONArray("tracks");
                final JSONObject trackObject = trackDataSingle.getJSONObject(0);
                final String singleTrackBase64 = trackObject.getString("track");
                final AudioTrack singleAudioTrack = LavalinkUtil.toAudioTrack(singleTrackBase64);
                callback.trackLoaded(singleAudioTrack);
                break;
            case "PLAYLIST_LOADED":
                final JSONArray trackData = response.getJSONArray("tracks");
                final List<AudioTrack> tracks = new ArrayList<>();
                for (final Object track : trackData) {
                    final String trackBase64 = ((JSONObject) track).getString("track");
                    final AudioTrack audioTrack = LavalinkUtil.toAudioTrack(trackBase64);
                    tracks.add(audioTrack);
                }
                final JSONObject playlistInfo = response.getJSONObject("playlistInfo");
                final int selectedTrackId = playlistInfo.getInt("selectedTrack");
                final AudioTrack selectedTrack;
                if (selectedTrackId < tracks.size() && selectedTrackId >= 0) {
                    selectedTrack = tracks.get(selectedTrackId);
                } else {
                    if (tracks.size() == 0)
                        throw new IOException("Empty playlist");
                    selectedTrack = tracks.get(0);
                }
                final String playlistName = playlistInfo.getString("name");
                final BasicAudioPlaylist playlist = new BasicAudioPlaylist(playlistName, tracks, selectedTrack, true);
                callback.playlistLoaded(playlist);
                break;
            case "NO_MATCHES":
                callback.noMatches();
                break;
            case "LOAD_FAILED":
                final JSONObject exception = response.getJSONObject("exception");
                final String message = exception.getString("message");
                final FriendlyException.Severity severity = FriendlyException.Severity.valueOf(exception.getString("severity"));
                final FriendlyException friendlyException = new FriendlyException(message, severity, new Throwable());
                callback.loadFailed(friendlyException);
                break;
            default:
                throw new IOException("Invalid loadType: " + loadType);
        }
    }
}
