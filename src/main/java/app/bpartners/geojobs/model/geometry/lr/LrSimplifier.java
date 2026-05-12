package app.bpartners.geojobs.model.geometry.lr;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@RequiredArgsConstructor
public class LrSimplifier implements UnaryOperator<Polygon> {
  private final double angleThreshold;

  @Override
  public Polygon apply(Polygon polygon) {
    var coordinates = getNotClosedCoordinates(polygon);
    var firstPass = simplify(coordinates);

    var rotated = rotate(firstPass, firstPass.size() - 1);
    var secondPass = simplify(rotated);
    return toPolygon(secondPass);
  }

  private List<Coordinate> rotate(List<Coordinate> coordinates, int startIndex) {
    int n = coordinates.size();
    List<Coordinate> out = new ArrayList<>();

    for (int i = 0; i < n; i++) {
      out.add(coordinates.get((startIndex + i) % n));
    }

    return out;
  }

  private List<Coordinate> simplify(List<Coordinate> coordinates) {
    int n = coordinates.size();
    if (n <= 3) return coordinates;

    int i = 0;
    List<Coordinate> simplified = new ArrayList<>();
    while (i < n) {
      var start = coordinates.get(i);
      simplified.add(start);

      if (i >= n - 1) break;

      var next = coordinates.get(i + 1);
      var constraint = new LinearStretchConstraint(start, next, angleThreshold);
      i = getNextStretchPointIndex(coordinates, constraint, i + 2);
    }

    return simplified;
  }

  private int getNextStretchPointIndex(
      List<Coordinate> coordinates, LinearStretchConstraint constraint, int start) {
    int j = start;
    int n = coordinates.size();
    while (j < n && constraint.isAligned(coordinates.get(j))) {
      j++;
    }
    return j - 1;
  }

  private static List<Coordinate> getNotClosedCoordinates(Polygon polygon) {
    var coordinates = polygon.getCoordinates();
    return Arrays.asList(Arrays.copyOf(coordinates, coordinates.length - 1));
  }

  private static Polygon toPolygon(List<Coordinate> coordinates) {
    var result = new ArrayList<>(coordinates);
    result.add(coordinates.getFirst());
    return geometryFactory.createPolygon(result.toArray(Coordinate[]::new));
  }
}
