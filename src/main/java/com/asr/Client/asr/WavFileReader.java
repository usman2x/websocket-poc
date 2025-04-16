package com.asr.Client.asr;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class WavFileReader {

  public static byte[] readWavFile(String filePath) throws IOException, UnsupportedAudioFileException {
    File file = new File(filePath);
    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);

    // Get audio format details
    AudioFormat format = audioInputStream.getFormat();
    if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
      throw new IllegalArgumentException("Only PCM_SIGNED encoding is supported.");
    }

    // Read the entire audio data into a byte array
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int bytesRead;
    while ((bytesRead = audioInputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }

    audioInputStream.close();
    return outputStream.toByteArray();
  }
}