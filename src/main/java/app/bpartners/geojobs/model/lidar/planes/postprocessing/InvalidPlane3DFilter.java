package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.isCompact;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class InvalidPlane3DFilter implements Function<Collection<Plane3D>, List<Plane3D>> {
  private final double min2DArea;
  private final double compactness;
  private static final int MIN_VALID_POLYGON_POINTS_COUNT = 4;

  @Override
  public List<Plane3D> apply(Collection<Plane3D> planes) {
    return planes.stream().filter(this::isValid).toList();
  }

  private boolean isValid(Plane3D plane) {
    var coordinates = plane.getDelimitation().getCoordinates();
    if (coordinates.length < MIN_VALID_POLYGON_POINTS_COUNT) {
      return false;
    }

    if (plane.get2DArea() <= min2DArea) {
      return false;
    }

    return isCompact(plane.getDelimitation(), compactness);
  }
}
