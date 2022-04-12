package lavalink.client.io.filters;

import javax.annotation.CheckReturnValue;

@SuppressWarnings("unused")
public class Distortion{
    private float sinOffset = 0.0f;
    private float sinScale = 1.0f;
    private float cosOffset = 0.0f;
    private float cosScale = 1.0f;
    private float tanOffset = 0.0f;
    private float tanScale = 1.0f;
    private float offset = 0.0f;
    private float scale = 1.0f;

    public float getSinOffset(){
        return sinOffset;
    }

    @CheckReturnValue
    public Distortion setSinOffset(float sinOffset){
        this.sinOffset = sinOffset;
        return this;
    }

    public float getSinScale(){
        return sinScale;
    }

    @CheckReturnValue
    public Distortion setSinScale(float sinScale){
        this.sinScale = sinScale;
        return this;
    }

    public float getCosOffset(){
        return cosOffset;
    }

    @CheckReturnValue
    public Distortion setCosOffset(float cosOffset){
        this.cosOffset = cosOffset;
        return this;
    }

    public float getCosScale(){
        return cosScale;
    }

    @CheckReturnValue
    public Distortion setCosScale(float cosScale){
        this.cosScale = cosScale;
        return this;
    }

    public float getTanOffset(){
        return tanOffset;
    }

    @CheckReturnValue
    public Distortion setTanOffset(float tanOffset){
        this.tanOffset = tanOffset;
        return this;
    }

    public float getTanScale(){
        return tanScale;
    }

    @CheckReturnValue
    public Distortion setTanScale(float tanScale){
        this.tanScale = tanScale;
        return this;
    }

    public float getOffset(){
        return offset;
    }

    @CheckReturnValue
    public Distortion setOffset(float offset){
        this.offset = offset;
        return this;
    }

    public float getScale(){
        return scale;
    }

    @CheckReturnValue
    public Distortion setScale(float scale){
        this.scale = scale;
        return this;
    }

}
