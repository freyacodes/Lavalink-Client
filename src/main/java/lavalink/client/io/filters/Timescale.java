/*
 * Copyright (c) Freya Arbjerg. Licensed under the MIT license
 */

package lavalink.client.io.filters;

import javax.annotation.CheckReturnValue;

@SuppressWarnings("unused")
public class Timescale {
    private float speed = 1.0f;
    private float pitch= 1.0f;
    private float rate = 1.0f;

    public float getSpeed() {
        return speed;
    }

    @CheckReturnValue
    public Timescale setSpeed(float speed) {
        if (speed < 0) throw new IllegalArgumentException("Speed must be greater than 0");
        this.speed = speed;
        return this;
    }

    public float getPitch() {
        return pitch;
    }

    @CheckReturnValue
    public Timescale setPitch(float pitch) {
        if (pitch < 0) throw new IllegalArgumentException("Pitch must be greater than 0");
        this.pitch = pitch;
        return this;
    }

    public float getRate() {
        return rate;
    }

    @CheckReturnValue
    public Timescale setRate(float rate) {
        if (rate < 0) throw new IllegalArgumentException("Rate must be greater than 0");
        this.rate = rate;
        return this;
    }
}
