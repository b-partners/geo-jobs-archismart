package app.bpartners.geojobs.service.lidar.model.geometry.roof;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import lombok.Getter;
import org.locationtech.jts.geom.Polygon;

@Getter
public class RoofPlane3D extends Plane3D {
  private final Polygon roofPolygon;

  public RoofPlane3D(
      Polygon roofPolygon,
      Plane3D plane,
      double concaveRatio,
      double delimitationSimplificationEpsilon) {
    super(
        plane.getA(),
        plane.getB(),
        plane.getC(),
        plane.getD(),
        plane.getKernel(),
        plane.getPoints(),
        concaveRatio,
        delimitationSimplificationEpsilon,
        plane.getExporter());
    this.roofPolygon = roofPolygon;
    this.delimitation = plane.getDelimitation();
  }
}
