package app.bpartners.geojobs.model.geometry.area;

import java.util.function.Function;
import org.locationtech.jts.geom.Geometry;

public class AreaComputer implements Function<Geometry, Area> {

  @Override
  public SquareDegree apply(Geometry geometry) {
    return new SquareDegree(geometry.getArea());
  }
}
