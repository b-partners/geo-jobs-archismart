package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.intersection;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.*;
import static java.util.Comparator.comparingDouble;
import static java.util.function.Predicate.not;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.algorithm.OBB3DComputer;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.model.ChimneyPlane3D;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

@Slf4j
@RequiredArgsConstructor
public class ChimneyFixer implements Function<Collection<Plane3D>, List<Plane3D>> {
  private final double maxChimneyArea;
  private final OBB3DComputer obb3DComputer;
  private final Plane3DExtractionStepExporter exporter;
  private static final double ROOF_BUFFER_IN_METERS = 0.1;
  private static final double HEIGHT_TOLERANCE = 0.2;

  public ChimneyFixer(double maxChimneyArea, Plane3DExtractionStepExporter exporter) {
    this(maxChimneyArea, new OBB3DComputer(), exporter);
  }

  @Override
  public List<Plane3D> apply(Collection<Plane3D> planes) {
    var separated = separate(planes);
    List<Plane3D> result = new ArrayList<>(separated.others());

    log.info("chimmeys={}", separated.chimneys());
    var doExport = exporter != null;
    int i = 1;
    for (var chimney : separated.chimneys()) {
      var fixed = fix(chimney);
      result.add(new ChimneyPlane3D(fixed));

      if (doExport) {
        var subExporter = exporter.subSuffix(String.valueOf(i++));
        subExporter.export(CHIMNEY_POLYGON, chimney.getDelimitation());
        subExporter.export(CHIMNEY_FIXED_POLYGON, fixed.getDelimitation());
        subExporter.export(CHIMNEY_CONVEXE_POLYGON, chimney.getConvexDelimitation());
      }
    }

    return result;
  }

  public Plane3D fix(Plane3D chimney) {
    var obb3D = obb3DComputer.apply(chimney);
    var coordinates = obb3D.getCoordinates();
    var maxZ = Arrays.stream(coordinates).mapToDouble(Coordinate::getZ).max().orElseThrow();

    var newCoordinates =
        Arrays.stream(coordinates)
            .map(coordinate -> new Coordinate(coordinate.getX(), coordinate.getY(), maxZ))
            .toArray(Coordinate[]::new);

    var delimitation = geometryFactory.createPolygon(newCoordinates);
    return chimney.toBuilder().delimitation(delimitation).area(null).build();
  }

  private SeparatorResult separate(Collection<Plane3D> planes) {
    var planesWithBigAreas = getPlanesWithBigAreasSortedByArea(planes, maxChimneyArea);
    var planesWithSmallAreas = getPlanesWithSmallAreas(planes, maxChimneyArea);
    var result = new SeparatorResult(new ArrayList<>(), new ArrayList<>(planesWithBigAreas));

    for (var small : planesWithSmallAreas) {
      if (isAChimney(small, planesWithBigAreas)) {
        result.chimneys().add(small);
      } else {
        result.others().add(small);
      }
    }
    return result;
  }

  static List<Plane3D> getPlanesWithBigAreasSortedByArea(
      Collection<Plane3D> planes, double minArea) {
    return planes.stream()
        .filter(plane -> plane.getArea() > minArea)
        .sorted(comparingDouble(Plane3D::get2DArea).reversed())
        .toList();
  }

  private static boolean isAChimney(Plane3D small, Collection<Plane3D> bigPlanes) {
    var smallDelimitation = small.getDelimitation();

    return bigPlanes.stream()
        .anyMatch(
            big -> {
              var bigDelimitation = big.getDelimitation();
              var bigDelimitationWithBuffer = bigDelimitation.buffer(ROOF_BUFFER_IN_METERS);
              if (!smallDelimitation.intersects(bigDelimitationWithBuffer)) {
                return false;
              }
              var intersection = intersection(smallDelimitation, bigDelimitationWithBuffer);
              if (intersection.isEmpty() || intersection instanceof Point) {
                return false;
              }

              var smallMaxZ = getMaxZ(smallDelimitation);
              var bigZ = big.zAt(smallMaxZ.getX(), smallMaxZ.getY());
              if (smallMaxZ.getZ() > bigZ) {
                return true;
              }

              return bigZ - smallMaxZ.getZ() <= HEIGHT_TOLERANCE;
            });
  }

  static List<Plane3D> getPlanesWithSmallAreas(Collection<Plane3D> planes, double maxArea) {
    return planes.stream().filter(not(plane -> plane.getArea() > maxArea)).toList();
  }

  static Coordinate getMaxZ(Geometry polygon) {
    return Arrays.stream(polygon.getCoordinates())
        .max(comparingDouble(Coordinate::getZ))
        .orElseThrow();
  }

  private record SeparatorResult(List<Plane3D> chimneys, List<Plane3D> others) {}
}
