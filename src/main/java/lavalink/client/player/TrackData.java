/*
 * Copyright (c) Freya Arbjerg. Licensed under the MIT license
 */

package lavalink.client.player;
/**
 * Created by napster on 25.09.17.
 * <p>
 * Optional object to enrich an AudioTrack via {@code AudioTrack#setUserData}
 */
public class TrackData {

    public final long startPos;
    public final long endPos;

    public TrackData(long startPos, long endPos) {
        this.startPos = startPos;
        this.endPos = endPos;
    }
}
