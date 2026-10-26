package app.bpartners.geojobs.utils.it;

import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.endpoint.rest.model.Point.TypeEnum.POINT;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

/** Turns an {@link AddressPoint} into the GeoJSON payload the API expects. */
public final class AddressPointFeatures {
  private AddressPointFeatures() {}

  /**
   * The point feature of the given address, holding its {@code address} property. Coordinates are
   * written as {@code [longitude, latitude]}, as required by the GeoJSON spec, whereas {@link
   * AddressPoint} stores them the other way around.
   */
  public static Feature toPointFeature(AddressPoint point) {
    var properties = new HashMap<String, Object>();
    properties.put("address", point.address());
    return new Feature().type(FEATURE).properties(properties).geometry(toGeometry(point));
  }

  private static FeatureGeometry toGeometry(AddressPoint point) {
    return new FeatureGeometry(
        new Point()
            .type(POINT)
            .coordinates(
                List.of(
                    BigDecimal.valueOf(point.longitude()), BigDecimal.valueOf(point.latitude()))));
  }
}
