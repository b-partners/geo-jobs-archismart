package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.HashMap;
import java.util.List;
import org.locationtech.jts.geom.MultiPolygon;

public record RoofDetails(MultiPolygon latLonGeometry, List<String> addresses) {

  public Feature feature() {
    if (latLonGeometry == null) {
      return null;
    }
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("addresses", addresses);
    return new Feature()
        .type(FEATURE)
        .properties(properties)
        .geometry(
            new FeatureGeometry(
                new app.bpartners.geojobs.endpoint.rest.model.MultiPolygon()
                    .type(MULTI_POLYGON)
                    .coordinates(
                        new GeometryConverter().multiPolygonToNestedList(latLonGeometry))));
  }
}
