package app.bpartners.geojobs.endpoint.rest.postprocessing;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.geojson.feature.FeatureJSON;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public class GeoJsonLoader {
  private static final String DEFAULT_ROUTE_TYPE = "line";

  public Set<LatLonPolygon> apply(File geojsonPath) {
    Set<LatLonPolygon> latLonPolygons = new HashSet<>();

    var featureJson = new FeatureJSON();
    try (FileReader reader = new FileReader(geojsonPath)) {
      var featureCollection = featureJson.readFeatureCollection(reader);
      try (var featuresIterator = featureCollection.features()) {
        while (featuresIterator.hasNext()) {
          SimpleFeature feature = (SimpleFeature) featuresIterator.next();
          Polygon polygon;
          try {
            polygon = (Polygon) feature.getDefaultGeometry();
          } catch (ClassCastException e) {
            var multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
            if (multiPolygon.getNumGeometries() != 1) {
              throw new RuntimeException(
                  "Only mulitpolygons with single polygon supported but got: " + multiPolygon);
            }
            polygon = (Polygon) multiPolygon.getGeometryN(0);
          }
          var label =
              feature.getProperty("label") == null
                  ? DEFAULT_ROUTE_TYPE
                  : feature.getProperty("label").getValue();
          polygon.setUserData(Map.of("label", label));
          latLonPolygons.add(new LatLonPolygon(polygon));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return latLonPolygons;
  }
}
