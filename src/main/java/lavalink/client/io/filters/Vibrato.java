package lavalink.client.io.filters;

import javax.annotation.CheckReturnValue;

@SuppressWarnings("unused")
public class Vibrato {
    private float frequency = 2.0f;
    private float depth = 0.5f;

    public float getFrequency() {
        return frequency;
    }

    @CheckReturnValue
    public Vibrato setFrequency(float frequency) {
        if (frequency <= 0 || frequency >= 14) throw new IllegalArgumentException("Frequency must be >0 and <=14");
        this.frequency = frequency;
        return this;
    }

    public float getDepth() {
        return depth;
    }

    @CheckReturnValue
    public Vibrato setDepth(float depth) {
        if (depth <= 0 || depth >= 1) throw new IllegalArgumentException("Depth must be >0 and <=1");
        this.depth = depth;
        return this;
    }
}
