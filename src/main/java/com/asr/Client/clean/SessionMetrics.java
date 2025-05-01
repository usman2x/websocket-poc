package com.asr.Client.clean;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SessionMetrics {
  private final Instant startTime;
  private Instant endTime;

  private final AtomicInteger messageCount = new AtomicInteger();
  private final AtomicLong totalBytes = new AtomicLong();

  public SessionMetrics() {
    this.startTime = Instant.now();
  }

  public void logMessage(int bytes) {
    messageCount.incrementAndGet();
    totalBytes.addAndGet(bytes);
  }

  public void end() {
    this.endTime = Instant.now();
  }

  public void printSummary(String sessionId) {
    Duration duration = (endTime != null ? Duration.between(startTime, endTime) : Duration.ZERO);
    System.out.printf(
        "\nSession [%s] Metrics:\n" +
            "Duration: %d sec\n" +
            "Messages: %d\n" +
            "Total Bytes: %d\n",
        sessionId,
        duration.getSeconds(),
        messageCount.get(),
        totalBytes.get()
    );
  }
}
