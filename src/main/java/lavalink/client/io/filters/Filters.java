package lavalink.client.io.filters;

import lavalink.client.player.LavalinkPlayer;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer.BAND_COUNT;

@SuppressWarnings("unused")
public class Filters {
    private final LavalinkPlayer player;
    private float volume = 1.0f;
    private float[] bands = new float[BAND_COUNT];
    private Karaoke karaoke = null;
    private Timescale timescale = null;
    private Trebolo trebolo = null;
    private Vibrato vibrato = null;

    /**
     * Intended for internal use only
     */
    public Filters(LavalinkPlayer player) {
        this.player = player;
    }

    @Nonnull
    public LavalinkPlayer getPlayer() {
        return player;
    }

    public float[] getBands() {
        return bands;
    }

    /**
     * Configures the equalizer.
     *
     * @param band the band to change, values 0-14
     * @param gain the gain in volume for the given band, range -0.25 (mute) to 1.0 (quadruple).
     */
    @CheckReturnValue
    public Filters setBand(int band, float gain) {
        if (gain < -0.25 || gain > 1) throw new IllegalArgumentException("Gain must be -0.25 to 1.0");
        bands[band] = gain;
        return this;
    }

    public float getVolume() {
        return volume;
    }

    /**
     * @param volume where 1.0f is regular volume. Values greater than 1.0f are allowed, but may cause clipping.
     */
    @CheckReturnValue
    public Filters setVolume(float volume) {
        if (volume < 0) throw new IllegalArgumentException("Volume must be greater than 0");
        this.volume = volume;
        return this;
    }

    @Nullable
    public Karaoke getKaraoke() {
        return karaoke;
    }

    @CheckReturnValue
    public Filters setKaraoke(Karaoke karaoke) {
        this.karaoke = karaoke;
        return this;
    }

    @Nullable
    public Timescale getTimescale() {
        return timescale;
    }

    @CheckReturnValue
    public Filters setTimescale(Timescale timescale) {
        this.timescale = timescale;
        return this;
    }

    @Nullable
    public Trebolo getTrebolo() {
        return trebolo;
    }

    @CheckReturnValue
    public Filters setTrebolo(Trebolo trebolo) {
        this.trebolo = trebolo;
        return this;
    }

    @Nullable
    public Vibrato getVibrato() {
        return vibrato;
    }

    @CheckReturnValue
    public Filters setVibrato(Vibrato vibrato) {
        this.vibrato = vibrato;
        return this;
    }

    /**
     * Commits these filters to the Lavalink server.
     *
     * The client may choose to commit changes at any time, even if this method is never invoked.
     */
    public void commit() {
        // TODO
    }

}
