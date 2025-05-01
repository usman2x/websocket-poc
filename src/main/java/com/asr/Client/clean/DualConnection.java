package com.asr.Client.clean;

import com.neovisionaries.ws.client.WebSocket;
import org.springframework.web.socket.WebSocketSession;

public class DualConnection {
    public WebSocketSession clientSession;
    public WebSocket asrSocket;

    public DualConnection(WebSocketSession clientSession, WebSocket asrSocket) {
        this.clientSession = clientSession;
        this.asrSocket = asrSocket;
    }
}
