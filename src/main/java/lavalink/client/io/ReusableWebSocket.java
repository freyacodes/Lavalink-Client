/*
 * Copyright (c) Freya Arbjerg. Licensed under the MIT license
 */

package lavalink.client.io;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

public abstract class ReusableWebSocket {

    private static final Logger log = LoggerFactory.getLogger(ReusableWebSocket.class);

    private DisposableSocket socket;
    private final URI serverUri;
    private final Draft draft;
    private final Map<String, String> headers;
    private final int connectTimeout;
    private final ReusableWebSocket instance = this; // For use in inner class
    private boolean isUsed = false;
    private int heartbeatTimeout = 60;

    public ReusableWebSocket(URI serverUri, Draft draft, Map<String, String> headers, int connectTimeout) {
        this.serverUri = serverUri;
        this.draft = draft;
        this.headers = headers;
        this.connectTimeout = connectTimeout;
    }

    public abstract void onOpen(ServerHandshake handshakeData);

    public abstract void onMessage(String message);

    public abstract void onClose(int code, String reason, boolean remote);

    public abstract void onError(Exception ex);

    public void send(String text) {
        if (socket != null && socket.isOpen()) {
            socket.send(text);
        }
    }

    public URI getServerUri() {
        return this.serverUri;
    }

    //will return null if there is no connection
    public InetSocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    public boolean isOpen() {
        return socket != null && socket.isOpen();
    }

    public boolean isConnecting() {
        return socket != null && !socket.isOpen() && !socket.isClosed() && !socket.isClosing();
    }

    public boolean isClosed() {
        return socket == null || socket.isClosed();
    }

    public boolean isClosing() {
        return socket != null && socket.isClosing();
    }

    public void connect() {
        if (socket == null || isUsed) socket = new DisposableSocket(serverUri, draft, headers, connectTimeout);
        socket.setConnectionLostTimeout(heartbeatTimeout);
        socket.connect();
        isUsed = true;
    }

    public void connectBlocking() throws InterruptedException {
        if (socket == null || isUsed) socket = new DisposableSocket(serverUri, draft, headers, connectTimeout);
        socket.setConnectionLostTimeout(heartbeatTimeout);
        socket.connectBlocking();
        isUsed = true;
    }

    public void close() {
        if (socket != null)
            socket.close();
    }

    public void close(int code) {
        if (socket != null)
            socket.close(code);
    }

    public void close(int code, String reason) {
        if (socket != null)
            socket.close(code, reason);
    }

    public void setHeartbeatTimeout(int seconds) {
        heartbeatTimeout = seconds;
        socket.setConnectionLostTimeout(seconds);
    }

    private class DisposableSocket extends WebSocketClient {

        DisposableSocket(URI serverUri, Draft protocolDraft, Map<String, String> httpHeaders, int connectTimeout) {
            super(serverUri, protocolDraft, httpHeaders, connectTimeout);
            isUsed = false;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            instance.onOpen(handshakedata);
        }

        @Override
        public void onMessage(String message) {
            instance.onMessage(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            instance.onClose(code, reason, remote);
        }

        @Override
        public void onError(Exception ex) {
            instance.onError(ex);
        }
    }

}
