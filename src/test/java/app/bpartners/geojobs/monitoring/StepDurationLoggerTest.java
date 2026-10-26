package app.bpartners.geojobs.monitoring;

import static app.bpartners.geojobs.monitoring.StepDurationLogger.logDurationOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class StepDurationLoggerTest {
  @Test
  void returns_the_value_produced_by_the_timed_step() {
    var actual = logDurationOf("a supplying step", () -> "computed");

    assertEquals("computed", actual);
  }

  @Test
  void runs_a_step_that_produces_nothing() {
    var ran = new AtomicBoolean(false);

    logDurationOf("a running step", () -> ran.set(true));

    assertTrue(ran.get());
  }

  @Test
  void propagates_the_failure_of_the_timed_step() {
    var thrown =
        assertThrows(
            IllegalStateException.class,
            () ->
                logDurationOf(
                    "a failing step",
                    () -> {
                      throw new IllegalStateException("community is not authorized");
                    }));

    assertEquals("community is not authorized", thrown.getMessage());
  }
}
