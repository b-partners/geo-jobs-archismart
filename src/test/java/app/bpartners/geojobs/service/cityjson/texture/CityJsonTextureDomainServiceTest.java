package app.bpartners.geojobs.service.cityjson.texture;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_2056;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_3812;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CityJsonTextureDomainServiceTest {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final GeometrySquareMeterArea projector = new GeometrySquareMeterArea();
  private static final CityJsonTextureDomainService subject =
      new CityJsonTextureDomainService(objectMapper, projector);

  @ParameterizedTest(name = "referenceSystem \"{0}\" resolves to {1}")
  @MethodSource("referenceSystems")
  void resolvesCrsFromReferenceSystem(
      String referenceSystem, CoordinateReferenceSystem expectedCrs) {
    var cityJson = cityJsonWithReferenceSystem(referenceSystem);

    var actual = subject.toCityJsonWithVertices(cityJson);

    assertEquals(expectedCrs, actual.crs());
  }

  private static Stream<Arguments> referenceSystems() {
    return Stream.of(
        arguments(null, LAMBERT_93),
        arguments("https://www.opengis.net/def/crs/EPSG/0/4326", WGS84),
        arguments("http://www.opengis.net/def/crs/EPSG/0/2154", LAMBERT_93),
        arguments("https://www.opengis.net/def/crs/EPSG/0/2056", EPSG_2056),
        arguments("https://www.opengis.net/def/crs/EPSG/0/3812", EPSG_3812),
        arguments("https://www.opengis.net/def/crs/EPSG/0/9999", LAMBERT_93));
  }

  private static ObjectNode cityJsonWithReferenceSystem(String referenceSystem) {
    var json = objectMapper.createObjectNode();
    json.set("vertices", objectMapper.createArrayNode());

    if (referenceSystem != null) {
      var metadata = objectMapper.createObjectNode();
      metadata.put("referenceSystem", referenceSystem);
      json.set("metadata", metadata);
    }

    return json;
  }
}
