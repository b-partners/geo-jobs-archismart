package app.bpartners.geojobs.model.geometry.area;

import lombok.Getter;
import lombok.Value;

@Getter
@Value
public class SquareDegree extends Area {
  double value;

  public MetricArea toSquareMeter(Object projection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int compareTo(Area that) {
    if (!(that instanceof SquareDegree thatSquareDegree)) {
      throw new UnsupportedOperationException();
    }

    return (int)
        ((value - thatSquareDegree.value) * 1_000_000_000 /*as degree areas are pretty small*/);
  }
}
