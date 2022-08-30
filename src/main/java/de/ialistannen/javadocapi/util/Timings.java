package de.ialistannen.javadocapi.util;

import com.google.gson.Gson;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.function.FailableSupplier;

public class Timings {

  private final Map<String, Instant> openTimings;
  private final Map<String, Duration> doneTimings;

  public Timings() {
    this.openTimings = new HashMap<>();
    this.doneTimings = new HashMap<>();
  }

  public <T, E extends Throwable> T measure(String name, FailableSupplier<T, E> supplier) throws E {
    startTimer(name);
    T value = supplier.get();
    stopTimer(name);
    return value;
  }
  public <E extends Throwable> void measure(String name, FailableRunnable<E> action) throws E {
    startTimer(name);
    action.run();
    stopTimer(name);
  }

  public void startTimer(String name) {
    openTimings.put(name, Instant.now());
  }

  public void stopTimer(String name) {
    Instant end = Instant.now();
    Instant start = Objects.requireNonNull(openTimings.remove(name));
    doneTimings.put(name, Duration.between(start, end));
  }

  public String serialize(Gson gson) {
    if (!openTimings.isEmpty()) {
      System.err.println("There are still open timings " + openTimings);
    }

    Map<String, Long> data = new HashMap<>();

    for (Entry<String, Duration> entry : doneTimings.entrySet()) {
      data.put(entry.getKey(), entry.getValue().toMillis());
    }

    return gson.toJson(data);
  }
}
