package com.asr.Client.config;


import com.asr.Client.asr.MyBinaryWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
public class WebSocketClientConfig {

  private static final String SERVER_URI = "ws://35.188.7.104:8766";

  @Bean
  public WebSocketConnectionManager webSocketConnectionManager() {
    WebSocketConnectionManager manager = new WebSocketConnectionManager(
        webSocketClient(),
        new MyBinaryWebSocketHandler(),
        SERVER_URI
    );
    manager.setAutoStartup(true);
    return manager;
  }

  @Bean
  public StandardWebSocketClient webSocketClient() {
    return new StandardWebSocketClient();
  }
}