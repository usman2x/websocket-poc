package com.asr.Client.streamer;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioStreamer {

  private static final int SAMPLE_RATE = 16000;
  private static final int ENCODER_STEP_LENGTH = 80; // Example value
  private static final int LOOKAHEAD_SIZE = 80; // Example value
  private static final int CHUNK_SIZE = LOOKAHEAD_SIZE + ENCODER_STEP_LENGTH;

  final int CHUNK_MS = 160;          // duration of each chunk in milliseconds
  final int CHANNELS = 1;
  final int BYTES_PER_SAMPLE = 2;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private WebSocketSession session;

  public AudioStreamer(WebSocketSession session) {
    this.session = session;
  }

  private boolean isSilent(byte[] audioData, int threshold) {
    // Convert byte array to short array (16-bit PCM)
    short[] samples = new short[audioData.length / 2];
    for (int i = 0; i < samples.length; i++) {
      samples[i] = (short) ((audioData[i * 2] & 0xFF) | (audioData[i * 2 + 1] << 8));
    }

    // Calculate the root mean square (RMS) of the samples
    double sumOfSquares = 0.0;
    for (short sample : samples) {
      sumOfSquares += sample * sample;
    }
    double rms = Math.sqrt(sumOfSquares / samples.length);

    // Return true if the RMS is below the threshold (silent)
    return rms < threshold;
  }

  public void startStreaming() {
    executor.submit(() -> {
      try {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
          System.err.println("Audio format not supported.");
          return;
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        System.out.println("Listening...");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bufferSize = (SAMPLE_RATE * CHUNK_MS / 1000) * BYTES_PER_SAMPLE * CHANNELS;
        byte[] buffer = new byte[bufferSize];

        while (true) {
          int bytesRead = line.read(buffer, 0, buffer.length);
          if (bytesRead > 0) {
            out.write(buffer, 0, bytesRead);
            if (out.size() >= CHUNK_SIZE * 2) {
              byte[] chunk = out.toByteArray();
              if (!isSilent(chunk, 500)) { // Adjust threshold as needed
                session.sendMessage(new BinaryMessage(ByteBuffer.wrap(chunk)));
                System.out.println("Sent non-silent chunk of size: " + chunk.length);
              } else {
                System.out.println("Silent chunk detected, skipping...");
              }
              out.reset();
              Thread.sleep(10);
            }
          }
        }
      } catch (Exception e) {
        System.err.println("Error in audio streaming: " + e.getMessage());
      }
    });
  }
}