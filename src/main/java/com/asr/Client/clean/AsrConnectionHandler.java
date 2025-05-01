package com.asr.Client.clean;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

@Component
public class AsrConnectionHandler extends WebSocketAdapter {
    @Autowired @Lazy
    private WebSocketConnectionManager connectionManager;

    @Override
    public void onTextMessage(WebSocket websocket, String message) throws Exception {
        System.out.println("Incoming Response from ASR: " + message);
        connectionManager.getConnection(websocket.getPingSenderName()).clientSession.sendMessage(
                new TextMessage(message)
        );
        System.out.println("Forwarded text to client.");
    }
}
