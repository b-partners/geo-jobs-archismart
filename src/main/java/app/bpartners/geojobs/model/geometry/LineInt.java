package app.bpartners.geojobs.model.geometry;

import org.locationtech.jts.geom.Coordinate;

public record LineInt(IntXY a, IntXY b) {
  public double length() {
    return new Coordinate(a.x(), a.y()).distance(new Coordinate(b.x(), b.y()));
  }

  public double angle() {
    double deltaX = b.x() - a.x();
    double deltaY = b.y() - a.y();
    return Math.atan2(deltaY, deltaX);
  }
}
