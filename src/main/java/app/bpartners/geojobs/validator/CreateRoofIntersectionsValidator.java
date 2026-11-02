package app.bpartners.geojobs.validator;

import app.bpartners.geojobs.endpoint.rest.model.CreateRoofIntersections;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateRoofIntersectionsValidator implements Consumer<CreateRoofIntersections> {
  @Override
  public void accept(CreateRoofIntersections createRoofIntersections) {
    if (createRoofIntersections == null
        || createRoofIntersections.getFeature() == null
        || createRoofIntersections.getFeature().getGeometry() == null) {
      throw new BadRequestException("Feature and its geometry are mandatory");
    }
    if (!(createRoofIntersections.getFeature().getGeometry().getActualInstance()
        instanceof Polygon)) {
      throw new BadRequestException(
          "Only a single Polygon is supported as roof intersections zone");
    }
  }
}
