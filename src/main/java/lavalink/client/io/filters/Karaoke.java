/*
 * Copyright (c) Freya Arbjerg. Licensed under the MIT license
 */

package lavalink.client.io.filters;

import javax.annotation.CheckReturnValue;

@SuppressWarnings("unused")
public class Karaoke {
    private float level = 1.0f;
    private float monoLevel = 1.0f;
    private float filterBand = 220.0f;
    private float filterWidth = 100.0f;

    public float getLevel() {
        return level;
    }

    @CheckReturnValue
    public Karaoke setLevel(float level) {
        this.level = level;
        return this;
    }

    public float getMonoLevel() {
        return monoLevel;
    }

    @CheckReturnValue
    public Karaoke setMonoLevel(float monoLevel) {
        this.monoLevel = monoLevel;
        return this;
    }

    public float getFilterBand() {
        return filterBand;
    }

    @CheckReturnValue
    public Karaoke setFilterBand(float filterBand) {
        this.filterBand = filterBand;
        return this;
    }

    public float getFilterWidth() {
        return filterWidth;
    }

    @CheckReturnValue
    public Karaoke setFilterWidth(float filterWidth) {
        this.filterWidth = filterWidth;
        return this;
    }
}
