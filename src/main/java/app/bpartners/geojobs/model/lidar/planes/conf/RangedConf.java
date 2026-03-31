package app.bpartners.geojobs.model.lidar.planes.conf;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public record RangedConf<T, V>(List<Value<T, V>> values) {
  public V getValue(T input) {
    for (var value : values) {
      if (value.matches(input)) {
        return value.getRaw();
      }
    }
    throw new IllegalArgumentException("No matching range for input: " + input);
  }

  @SafeVarargs
  public static <T, V> RangedConf<T, V> from(Value<T, V>... values) {
    return new RangedConf<>(List.of(values));
  }

  @Getter
  @RequiredArgsConstructor
  public abstract static class Value<T, V> {
    private final T min;
    private final T max;
    private final V raw;

    abstract boolean matches(T input);
  }

  public static class IntegerRangedConf<V> extends Value<Integer, V> {
    public IntegerRangedConf(Integer min, Integer max, V value) {
      super(min, max, value);
    }

    @Override
    boolean matches(Integer input) {
      return input >= getMin() && input <= getMax();
    }
  }
}
