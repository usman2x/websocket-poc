package com.asr.Client.middleware;

import com.neovisionaries.ws.client.WebSocket;
import org.springframework.web.socket.WebSocketSession;

public class ConnectionBridge {
    public WebSocketSession clientSession;
    public WebSocket asrSocket;

    public ConnectionBridge(WebSocketSession clientSession, WebSocket asrSocket) {
        this.clientSession = clientSession;
        this.asrSocket = asrSocket;
    }
}
