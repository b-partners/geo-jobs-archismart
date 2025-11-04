package app.bpartners.geojobs.service;

import static java.lang.Thread.sleep;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@AllArgsConstructor
public abstract class FalliblyDurableMockedFunction<T, R> implements Function<T, R> {

  private final Duration maxCallDuration;
  private final double failureRate;

  @SneakyThrows
  @Override
  public R apply(T t) {
    var random = new SecureRandom().nextDouble();
    sleep((long) (random * maxCallDuration.toMillis()));
    if (random < failureRate) {
      throw new RuntimeException("Oops, mock function randomly failed!");
    }

    return successfulMockedApply(t);
  }

  protected abstract R successfulMockedApply(T t);
}
