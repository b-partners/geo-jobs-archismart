package app.bpartners.geojobs.service.geojson;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class GeoJsonMultiPolygonCorrector implements Function<MultiPolygon, MultiPolygon> {

  @Override
  public MultiPolygon apply(MultiPolygon multiPolygon) {
    var fixedMultiPolygon = apply(Objects.requireNonNull(multiPolygon.getCoordinates()));
    multiPolygon.setCoordinates(fixedMultiPolygon);
    return multiPolygon;
  }

  private List<List<List<List<BigDecimal>>>> apply(
      List<List<List<List<BigDecimal>>>> multiPolygon) {
    List<List<List<List<BigDecimal>>>> fixedMultiPolygon = new ArrayList<>();

    for (List<List<List<BigDecimal>>> polygon : multiPolygon) {
      List<List<List<BigDecimal>>> fixedPolygon = new ArrayList<>();
      for (int i = 0; i < polygon.size(); i++) {
        List<List<BigDecimal>> ring = polygon.get(i);
        if (i == 0) {
          if (!isCounterClockwise(ring)) {
            Collections.reverse(ring);
          }
        } else {
          if (isCounterClockwise(ring)) {
            Collections.reverse(ring);
          }
        }
        fixedPolygon.add(ring);
      }
      fixedMultiPolygon.add(fixedPolygon);
    }
    return fixedMultiPolygon;
  }

  private boolean isCounterClockwise(List<List<BigDecimal>> ring) {
    BigDecimal sum = BigDecimal.ZERO;
    for (int i = 0; i < ring.size() - 1; i++) {
      BigDecimal x1 = ring.get(i).get(0);
      BigDecimal y1 = ring.get(i).get(1);
      BigDecimal x2 = ring.get(i + 1).get(0);
      BigDecimal y2 = ring.get(i + 1).get(1);
      sum = sum.add((x2.subtract(x1)).multiply(y2.add(y1)));
    }
    return sum.compareTo(BigDecimal.ZERO) < 0;
  }
}
