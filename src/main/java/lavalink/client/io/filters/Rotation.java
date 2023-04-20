/*
 * Copyright (c) Freya Arbjerg. Licensed under the MIT license
 */

package lavalink.client.io.filters;

import javax.annotation.CheckReturnValue;

@SuppressWarnings("unused")
public class Rotation{
    private float frequency = 2.0f;

    public float getFrequency() {
        return frequency;
    }

    @CheckReturnValue
    public Rotation setFrequency(float frequency) {
        this.frequency = frequency;
        return this;
    }

}
