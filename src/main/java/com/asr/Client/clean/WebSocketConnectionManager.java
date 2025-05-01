package com.asr.Client.clean;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketConnectionManager {

    @Value("${asr.websocket.url}")
    private String asrServer;

    private final Map<String, DualConnection> connections;
    private final AsrConnectionHandler asrHandler ;

    WebSocketConnectionManager(AsrConnectionHandler asrHandler) {
        connections = new ConcurrentHashMap<>();
        this.asrHandler = asrHandler;
    }

    public void addConnection(String key, DualConnection connection) {
        connections.put(key, connection);
    }

    public void deleteConnection(String key) {
        connections.remove(key);
    }

    public DualConnection getConnection(String key) {
        return connections.get(key);
    }

    public WebSocket createNewAsrConnection() throws Exception {
        WebSocketFactory factory = new WebSocketFactory();
        WebSocket ws = factory.createSocket(asrServer);
        ws.addListener(asrHandler);
        return ws.connect();
    }
}
