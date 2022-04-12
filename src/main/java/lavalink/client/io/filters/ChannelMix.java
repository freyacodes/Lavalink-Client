package lavalink.client.io.filters;

import javax.annotation.CheckReturnValue;

@SuppressWarnings("unused")
public class ChannelMix {
    private float leftToLeft = 1f;
    private float leftToRight = 0f;
    private float rightToLeft = 0f;
    private float rightToRight = 1f;

    public float getLeftToLeft() {
        return leftToLeft;
    }

    @CheckReturnValue
    public ChannelMix setLeftToLeft(float leftToLeft) {
        this.leftToLeft = leftToLeft;
        return this;
    }

    public float getLeftToRight() {
        return leftToRight;
    }

    @CheckReturnValue
    public ChannelMix setLeftToRight(float leftToRight) {
        this.leftToRight = leftToRight;
        return this;
    }

    public float getRightToLeft() {
        return rightToLeft;
    }

    @CheckReturnValue
    public ChannelMix setRightToLeft(float rightToLeft) {
        this.rightToLeft = rightToLeft;
        return this;
    }

    public float getRightToRight() {
        return rightToRight;
    }

    @CheckReturnValue
    public ChannelMix setRightToRight(float rightToRight) {
        this.rightToRight = rightToRight;
        return this;
    }
}
