package com.asr.Client.streamer;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.ByteBuffer;

public class AudioStreamerQueue {

  private static final int SAMPLE_RATE = 16000;
  private static final int ENCODER_STEP_LENGTH = 80; // Example value
  private static final int LOOKAHEAD_SIZE = 80; // Example value
  private static final int CHUNK_SIZE = LOOKAHEAD_SIZE + ENCODER_STEP_LENGTH;

  final int CHUNK_MS = 160;          // duration of each chunk in milliseconds
  final int CHANNELS = 1;
  final int BYTES_PER_SAMPLE = 2;

  private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
  private final ExecutorService executor = Executors.newFixedThreadPool(2);
  private WebSocketSession session;

  public AudioStreamerQueue(WebSocketSession session) {
    this.session = session;
  }

  public void startStreaming() {
    startAudioCapture();
    startWebSocketSender();
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

  private void startAudioCapture() {
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
              if (!isSilent(chunk, 250)) {
                audioQueue.put(chunk);
                System.out.println("Sent non-silent chunk of size: " + chunk.length);
              } else {
                System.out.println("Silent chunk detected, skipping...");
              }
            }
          }
        }
      } catch (Exception e) {
        System.err.println("Error in audio capture: " + e.getMessage());
      }
    });
  }

  private void startWebSocketSender() {
    executor.submit(() -> {
      try {
        while (true) {
          byte[] chunk = audioQueue.take(); // Retrieve a chunk from the queue
          if (chunk.length > 0) {
            session.sendMessage(new BinaryMessage(ByteBuffer.wrap(chunk)));
            System.out.println("Sent audio chunk of size: " + chunk.length);
          }
          Thread.sleep(CHUNK_MS); // Match the chunk duration
        }
      } catch (Exception e) {
        System.err.println("Error in WebSocket sender: " + e.getMessage());
      }
    });
  }
}