package app.bpartners.geojobs.service.geojson;

import java.util.function.Function;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.springframework.stereotype.Component;

@Component
public class GeometryCorrector implements Function<Geometry, Geometry> {
  @Override
  public Geometry apply(Geometry geometry) {
    // Validate/repair before precision reduction: in JTS 1.19 GeometryPrecisionReducer.reduce
    // goes through OverlayNG, which throws "Reduction failed, possible invalid input" on invalid
    // geometry (e.g. self-intersecting detected objects) instead of letting buffer(0) repair it.
    var validGeometry = geometry.isValid() ? geometry.copy() : GeometryFixer.fix(geometry);
    var precisionModel = new PrecisionModel(1e6);
    try {
      return GeometryPrecisionReducer.reduce(validGeometry, precisionModel).buffer(0);
    } catch (IllegalArgumentException e) {
      // reducePointwise never throws; buffer(0) repairs the resulting topology
      return GeometryPrecisionReducer.reducePointwise(validGeometry, precisionModel).buffer(0);
    }
  }
}
