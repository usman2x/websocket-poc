package com.asr.Client.clean;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ClientConnectionHandler extends BinaryWebSocketHandler {
    private final WebSocketConnectionManager connectionManager;
    private final Map<String, SessionMetrics> sessionMetricsMap = new ConcurrentHashMap<>();


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Connection Established: " + session.getId());
        session.setBinaryMessageSizeLimit(2048*2024);
        session.setTextMessageSizeLimit(2048*2024);

        var asrSocket = connectionManager.createNewAsrConnection();
        asrSocket.setPingSenderName(session.getId());
        connectionManager.addConnection(session.getId(), new DualConnection(session, asrSocket));

        sessionMetricsMap.put(session.getId(), new SessionMetrics());

    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("Connection Error: " + exception.getMessage());

        SessionMetrics metrics = sessionMetricsMap.remove(session.getId());
        if (metrics != null) {
            metrics.end();
            metrics.printSummary(session.getId());
        }

        connectionManager.deleteConnection(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        System.out.println("Connection Closed: " + closeStatus.getCode());

        SessionMetrics metrics = sessionMetricsMap.remove(session.getId());
        if (metrics != null) {
            metrics.end();
            metrics.printSummary(session.getId());
        }

        connectionManager.deleteConnection(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        var connection = connectionManager.getConnection(session.getId());
        int size = message.getPayload().remaining();
        System.out.printf("[%s] Incoming binary: %d bytes%n", session.getId(), size);


        sessionMetricsMap.get(session.getId()).logMessage(size);
        connection.asrSocket.sendBinary(message.getPayload().array());
        System.out.println("Forwarded binary to ASR server.");
    }

}
