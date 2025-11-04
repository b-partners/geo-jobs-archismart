package app.bpartners.geojobs.model.geometry.polygon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Polygon;

@AllArgsConstructor
public class PolygonEdges implements Supplier<List<LineSegment>> {

  private final Polygon polygon;

  @Override
  public List<LineSegment> get() {
    List<LineSegment> res = new ArrayList<>();

    var coordinates = polygon.getExteriorRing().getCoordinates();
    for (int i = 0; i < coordinates.length - 1 /* Last elt = first in ring */; i++) {
      res.add(new LineSegment(coordinates[i], coordinates[i + 1]));
    }
    return res;
  }
}
