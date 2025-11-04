package app.bpartners.geojobs.model.geometry.area;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@Value
public class MetricArea extends Area {

  double value;
  MetricAreaUnit unit;

  @Override
  public int compareTo(Area that) {
    if (!(that instanceof MetricArea thatSquareMeter)) {
      throw new UnsupportedOperationException();
    }

    return (int)
        (value * unit.getMultiplierToM2()
            - thatSquareMeter.value * thatSquareMeter.unit.getMultiplierToM2());
  }

  @AllArgsConstructor
  @Getter
  public enum MetricAreaUnit {
    m2(1),
    are(1_00),
    ha(1_00_00),
    km2(1_00_00_00);

    private final int multiplierToM2;
  }
}
