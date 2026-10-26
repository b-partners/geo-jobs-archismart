package app.bpartners.geojobs.monitoring;

import static java.time.Instant.now;

import java.time.Duration;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepDurationLogger {
  private StepDurationLogger() {}

  public static <T> T logDurationOf(String stepName, Supplier<T> step) {
    var start = now();
    try {
      return step.get();
    } finally {
      log.info(
          "[sync-detection] {} took {} ms", stepName, Duration.between(start, now()).toMillis());
    }
  }

  public static void logDurationOf(String stepName, Runnable step) {
    logDurationOf(
        stepName,
        () -> {
          step.run();
          return null;
        });
  }
}
