package app.bpartners.geojobs.model.lidar.planes.postprocessing.model;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;

public class ChimneyPlane3D extends Plane3D {
  public ChimneyPlane3D(Plane3D plane) {
    super(
        plane.getA(),
        plane.getB(),
        plane.getC(),
        plane.getD(),
        plane.getKernel(),
        plane.getPoints(),
        plane.getDelimitationConf(),
        plane.getExporter());
    this.delimitation = plane.getDelimitation();
  }
}
