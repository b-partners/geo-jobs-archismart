package app.bpartners.geojobs.service.geojson;

import java.util.function.Function;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.springframework.stereotype.Component;

@Component
public class GeometryCorrector implements Function<Geometry, Geometry> {
  @Override
  public Geometry apply(Geometry geometry) {
    return GeometryPrecisionReducer.reduce(geometry.copy(), new PrecisionModel(1e6)).buffer(0);
  }
}
