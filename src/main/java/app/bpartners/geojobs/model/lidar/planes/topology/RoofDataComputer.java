package app.bpartners.geojobs.model.lidar.planes.topology;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.extend;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.project;
import static app.bpartners.geojobs.model.lidar.planes.topology.algorithm.LineAndPolygonSnappingComputer.snap;
import static app.bpartners.geojobs.model.lidar.planes.topology.algorithm.PolygonSplitter.split;
import static app.bpartners.geojobs.model.lidar.planes.topology.model.RoofRelationType.*;
import static java.util.Comparator.comparingDouble;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.topology.model.RoofRelationType;
import app.bpartners.geojobs.model.lidar.planes.topology.model.RoofTopology;
import app.bpartners.geojobs.model.lidar.planes.topology.model.Rupture;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

public class RoofDataComputer implements BiConsumer<List<Plane3D>, RoofTopology> {
  private static final double O_EXTENSION = 20;
  private static final double S_EXTENSION = 1;
  private static final double MAX_DISTANCE = 3;

  @Override
  public void accept(List<Plane3D> planes, RoofTopology topology) {
    computeClip(List.of(O_PLUS, O_MINUS), O_EXTENSION, planes, topology);
    computeClip(List.of(S), S_EXTENSION, planes, topology);

    computeSnap(planes, topology);
  }

  private void computeClip(
      List<RoofRelationType> relations,
      double extension,
      List<Plane3D> planes,
      RoofTopology topology) {
    int n = planes.size();
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) continue;
        if (!topology.getAdjacency()[i][j]) continue;

        var relation = topology.getRelations()[i][j];
        if (!relations.contains(relation)) continue;

        var plane = planes.get(i);
        var rupture = topology.getRuptures()[i][j];
        var clipper = getClipper(rupture);
        if (hasPointsFar(clipper, plane)) {
          continue;
        }
        var clipped = clip(plane, extend(clipper, extension));
        planes.set(i, clipped);
      }
    }
  }

  private void computeSnap(List<Plane3D> planes, RoofTopology topology) {
    int n = planes.size();
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) continue;
        if (!topology.getAdjacency()[i][j]) continue;

        var plane = planes.get(i);
        var rupture = topology.getRuptures()[i][j];
        var clipper = getClipper(rupture);
        if (hasPointsFar(clipper, plane)) {
          continue;
        }
        var snapped = snap(plane, clipper);
        planes.set(i, snapped);
      }
    }
  }

  private static Plane3D clip(Plane3D plane, LineString clipper) {
    var delimitation = plane.getDelimitation();
    var clippedDelimitation = getClipped(delimitation, clipper);
    clippedDelimitation = project(plane, clippedDelimitation);
    return plane.toBuilder()
        .area(null)
        .convexDelimitation(null)
        .delimitation(clippedDelimitation)
        .build();
  }

  private static boolean hasPointsFar(LineString clipper, Plane3D plane) {
    return Arrays.stream(clipper.getCoordinates())
        .anyMatch(
            coordinate -> {
              var point = new LasPointGeometry(coordinate);
              return plane.distance(point) > MAX_DISTANCE;
            });
  }

  private static Polygon getClipped(Polygon toClip, LineString clipper) {
    var splits = split(toClip, clipper);
    return splits.stream().max(comparingDouble(Polygon::getArea)).orElse(toClip);
  }

  private static LineString getClipper(Rupture rupture) {
    var start = rupture.getStart();
    var end = rupture.getEnd();
    return geometryFactory.createLineString(new Coordinate[] {start, end});
  }
}
