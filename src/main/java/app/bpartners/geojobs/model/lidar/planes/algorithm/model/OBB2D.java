package app.bpartners.geojobs.model.lidar.planes.algorithm.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import lombok.Builder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Builder
public record OBB2D(
    LasPointGeometry center, double area, double angle, double width, double height) {

  public Polygon toPolygon() {
    double hw = width / 2;
    double hh = height / 2;

    double[][] localCoordinates = {
      {-hw, -hh},
      {hw, -hh},
      {hw, hh},
      {-hw, hh}
    };

    var coordinates = new Coordinate[5];
    double cos = Math.cos(angle);
    double sin = Math.sin(angle);

    for (int i = 0; i < 4; i++) {
      double x = localCoordinates[i][0];
      double y = localCoordinates[i][1];

      double rx = x * cos - y * sin;
      double ry = x * sin + y * cos;

      coordinates[i] = new Coordinate(center.getX() + rx, center.getY() + ry);
    }

    coordinates[4] = coordinates[0];
    return geometryFactory.createPolygon(coordinates);
  }
}
