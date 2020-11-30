package lavalink.client.io.filters;

import javax.annotation.CheckReturnValue;

@SuppressWarnings("unused")
public class Tremolo {
    private float frequency = 2.0f;
    private float depth = 0.5f;

    public float getFrequency() {
        return frequency;
    }

    @CheckReturnValue
    public Tremolo setFrequency(float frequency) {
        if (frequency <= 0) throw new IllegalArgumentException("Frequency must be >0");
        this.frequency = frequency;
        return this;
    }

    public float getDepth() {
        return depth;
    }

    @CheckReturnValue
    public Tremolo setDepth(float depth) {
        if (depth <= 0 || depth >= 1) throw new IllegalArgumentException("Frequency must be >0 and <=1");
        this.depth = depth;
        return this;
    }
}
