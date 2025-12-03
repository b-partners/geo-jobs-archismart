package app.bpartners.geojobs.service.ign;

import static app.bpartners.geojobs.service.geojson.GeometryConverter.staticWriteGeometryAsString;
import static org.springframework.http.HttpMethod.GET;

import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.ign.schemas.IgnFeature;
import app.bpartners.geojobs.service.ign.schemas.IgnResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class IgnCadastreFeatureFetcher implements Function<Geometry, List<Feature>> {
  private static final String URL = "https://apicarto.ign.fr/api/cadastre/parcelle?geom={geom}";

  private final RestTemplate restTemplate;

  @Override
  public List<Feature> apply(Geometry geometry) {
    String geoJson;
    if (geometry.getDimension() < 2) {
      geoJson = staticWriteGeometryAsString(createBoundingBoxAroundOneMeter(geometry));
    } else {
      geoJson = staticWriteGeometryAsString(geometry);
    }
    var response = restTemplate.exchange(URL, GET, null, IgnResponse.class, geoJson);

    var responseBody = response.getBody();
    if (responseBody == null || responseBody.features == null || responseBody.features.isEmpty()) {
      throw new IllegalStateException(
          "Unable to convert geometry " + geometry + " to Feature as empty features obtained");
    }
    return responseBody.features.stream().map(this::mapToFeature).toList();
  }

  @SneakyThrows
  private Feature mapToFeature(IgnFeature ignFeature) {
    var ignGeometryJsonValue = new ObjectMapper().writeValueAsString(ignFeature.geometry);
    return Feature.builder()
        .properties(
            ignFeature.properties == null ? new HashMap<>() : new HashMap<>(ignFeature.properties))
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(retrieveGeometryType(ignGeometryJsonValue))
                .actualInstanceStringValue(ignGeometryJsonValue)
                .build())
        .build();
  }

  private app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum retrieveGeometryType(
      String ignGeometry) {
    if (ignGeometry == null) {
      throw new UnsupportedOperationException(
          "Unable to retrieve GeometryType from " + ignGeometry);
    }
    if (ignGeometry.contains("MultiPolygon")) {
      return app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
    } else if (ignGeometry.contains("Polygon")) {
      return app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
    }
    throw new UnsupportedOperationException("Unable to retrieve GeometryType from " + ignGeometry);
  }

  private Geometry createBoundingBoxAroundOneMeter(Geometry geometry) {
    Envelope env = geometry.getEnvelopeInternal();
    double lat = geometry.getCoordinate().y;
    double metersToDegLat = 1.0 / 111320.0;
    double metersToDegLon = 1.0 / (111320.0 * Math.cos(Math.toRadians(lat)));
    Envelope expanded =
        new Envelope(
            env.getMinX() - metersToDegLon,
            env.getMaxX() + metersToDegLon,
            env.getMinY() - metersToDegLat,
            env.getMaxY() + metersToDegLat);
    GeometryFactory factory = geometry.getFactory();
    return factory.toGeometry(expanded);
  }
}
