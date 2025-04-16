package com.asr.Client.asr;
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

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private WebSocketSession session;

  public AudioStreamer(WebSocketSession session) {
    this.session = session;
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
        byte[] buffer = new byte[line.getBufferSize() / 5];

        while (true) {
          int bytesRead = line.read(buffer, 0, buffer.length);
          if (bytesRead > 0) {
            out.write(buffer, 0, bytesRead);

            // Send chunks when enough data is collected
            if (out.size() >= CHUNK_SIZE * 2) { // Multiply by 2 for 16-bit audio
              byte[] chunk = out.toByteArray();
              session.sendMessage(new BinaryMessage(ByteBuffer.wrap(chunk)));
              out.reset();

              // Simulate real-time streaming with a 10ms delay
              Thread.sleep(10); // Adjust based on chunk size
            }
          }
        }
      } catch (Exception e) {
        System.err.println("Error in audio streaming: " + e.getMessage());
      }
    });
  }

  public void startStreamingFromFile(String filePath) {
    executor.submit(() -> {
      try {
        byte[] audioData = WavFileReader.readWavFile(filePath);

        for (int i = 0; i < audioData.length; i += CHUNK_SIZE * 2) { // Multiply by 2 for 16-bit audio
          int length = Math.min(CHUNK_SIZE * 2, audioData.length - i);
          byte[] chunk = new byte[length];
          System.arraycopy(audioData, i, chunk, 0, length);

          session.sendMessage(new BinaryMessage(ByteBuffer.wrap(chunk)));
          System.out.println("Sent audio chunk of size: " + chunk.length);

          // Simulate real-time streaming with a 10ms delay
          Thread.sleep(10);
        }

        // Send end-of-stream signal
        session.sendMessage(new BinaryMessage(ByteBuffer.wrap(new byte[0])));
        System.out.println("Sent end-of-stream signal.");
      } catch (Exception e) {
        System.err.println("Error in audio streaming: " + e.getMessage());
      }
    });
  }
}