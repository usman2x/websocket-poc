package com.asr.Client.asr;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioStreamerNew {

  private static final int SAMPLE_RATE = 16000;
  private static final int CHUNK_DURATION_MS = 10;
  private static final int BYTES_PER_SAMPLE = 2;
  private static final int CHANNELS = 1;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final WebSocketSession session;

  public AudioStreamerNew(WebSocketSession session) {
    this.session = session;
  }

  public void startStreaming() {
    executor.submit(() -> {
      try {
        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            SAMPLE_RATE,
            16,
            CHANNELS,
            CHANNELS * BYTES_PER_SAMPLE,
            SAMPLE_RATE,
            false
        );

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
          System.err.println("Audio format not supported.");
          return;
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        System.out.println("Listening...");

        int chunkSize = SAMPLE_RATE / 1000 * CHUNK_DURATION_MS * BYTES_PER_SAMPLE;
        byte[] buffer = new byte[chunkSize];

        while (session.isOpen()) {
          int bytesRead = line.read(buffer, 0, buffer.length);
          if (bytesRead > 0) {
            session.sendMessage(new BinaryMessage(ByteBuffer.wrap(buffer, 0, bytesRead)));
          }
          Thread.sleep(CHUNK_DURATION_MS);
        }

        line.stop();
        line.close();

      } catch (Exception e) {
        System.err.println("Error in audio streaming: " + e.getMessage());
      }
    });
  }
}
