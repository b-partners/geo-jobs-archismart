package app.bpartners.geojobs.model.lidar.planes.topology.model;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.centroid;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class Rupture {
  private Coordinate end;
  private Coordinate start;

  private final int planeAIndex;
  private final int planeBIndex;
  private final LineString line;
  private final List<Coordinate> points;
  private final Set<Coordinate> endIntersection;
  private final Set<Coordinate> startIntersection;

  public Coordinate getStart() {
    if (start != null) {
      return start;
    }

    if (this.startIntersection.isEmpty()) {
      start = points.getFirst();
    } else {
      start = centroid(toPoints(startIntersection)).getCoordinate();
    }
    return start;
  }

  public Coordinate getEnd() {
    if (end != null) {
      return end;
    }

    if (this.endIntersection.isEmpty()) {
      end = points.getLast();
    } else {
      end = centroid(toPoints(endIntersection)).getCoordinate();
    }
    return end;
  }

  private static List<LasPointGeometry> toPoints(Set<Coordinate> coordinates) {
    return coordinates.stream().map(LasPointGeometry::new).toList();
  }
}
