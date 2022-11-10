/*
 * Copyright (c) Freya Arbjerg. Licensed under the MIT license
 */

package lavalink.client.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ReconnectTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ReconnectTask.class);
    private Lavalink lavalink;

    ReconnectTask(Lavalink lavalink) {
        this.lavalink = lavalink;
    }

    @Override
    public void run() {
        try {
            //noinspection unchecked
            List<LavalinkSocket> nodes = lavalink.getNodes();
            nodes.forEach(lavalinkSocket -> {
                if (lavalinkSocket.isClosed()
                        && !lavalinkSocket.isConnecting()
                        && System.currentTimeMillis() - lavalinkSocket.lastReconnectAttempt > lavalinkSocket.getReconnectInterval()) {
                    lavalinkSocket.attemptReconnect();
                }
            });
        } catch (Exception e) {
            log.error("Caught exception in reconnect thread", e);
        }
    }
}
