package lavalink.client.io.filters;

import javax.annotation.CheckReturnValue;

@SuppressWarnings("unused")
public class LowPass {
    private float smoothing = 20f;

    public float getSmoothing() {
        return smoothing;
    }

    @CheckReturnValue
    public LowPass setSmoothing(float smoothing) {
        this.smoothing = smoothing;
        return this;
    }
}
