package com.asr.Client.asr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

public class MyBinaryWebSocketHandler extends BinaryWebSocketHandler {

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
    System.out.println("Received message: " + new String(message.getPayload().array()));
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    System.out.println("Raw server response: " + message);
    String payload = message.getPayload();
    System.out.println("Received raw message from server: " + payload);
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode jsonNode = objectMapper.readTree(payload);
      String transcription = jsonNode.get("transcription").asText();
      System.out.println("Transcription: " + transcription);
    } catch (Exception e) {
      System.err.println("Failed to parse server response: " + e.getMessage());
    }
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    System.out.println("WebSocket connection established with server: " + session.getId());
    session.setBinaryMessageSizeLimit(2048*2024);
    session.setTextMessageSizeLimit(2048*2024);
    AudioStreamer audioStreamer = new AudioStreamer(session);
    audioStreamer.startStreaming();
//    audioStreamer.startStreamingFromFile("/Users/user/upwork/MesutToruk/Client/src/main/resources/voice.wav");
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
    System.out.println("WebSocket connection closed with status: " + status);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    exception.printStackTrace();
    System.out.println("Error during WebSocket connection");
  }

}