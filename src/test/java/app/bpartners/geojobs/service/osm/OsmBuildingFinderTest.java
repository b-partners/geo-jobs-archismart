package app.bpartners.geojobs.service.osm;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;

@Slf4j
class OsmBuildingFinderTest {
  ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  GeometryConverter geometryConverter = new GeometryConverter();
  NominatimClient nominatimClient = new NominatimClient(objectMapper);
  OsmBuildingFinder subject =
      new OsmBuildingFinder(nominatimClient, geometryConverter, objectMapper);

  @SneakyThrows
  @Test
  void geocode_address() {
    MultiPolygon multiPolygon;
    try {
      multiPolygon =
          subject.getBuildingMultiPolygon(
              List.of(
                  BigDecimal.valueOf(2.369347107300078), BigDecimal.valueOf(51.03112559258034)));

    } catch (Exception e) {
      log.error("error", e);
      assertTrue(1 == 1);
      return;
    }
    if (multiPolygon != null) {
      var feature =
          new GeometryConverter()
              .toFeature(randomUUID().toString(), 20, new HashMap<>(), multiPolygon);
      log.info(new ObjectMapper().writeValueAsString(toRestFeature(feature)));
      assertNotNull(feature);
    }
  }

  @Test
  void any_feature_retrieved_from_geocoding_for_location() {
    var locationAddress = "D86 Ldit Les Cotes - 07800 Saint-Georges-les-Bains";

    var actual = subject.geocodeAddress(locationAddress);

    assertNull(actual);
  }

  @Test
  void feature_retrieved_from_geocoding_for_only_correct_address() {
    var malFormedAddressWithLocation =
        "Zoning Industriel Moimont, 11 Rue Jean Jaurès - 95670 Marly-la-Ville";
    assertNull(subject.geocodeAddress(malFormedAddressWithLocation));

    var correctAddress = "11 Rue Jean Jaurès - 95670 Marly-la-Ville";
    var actual = subject.geocodeAddress(correctAddress);
    assertNotNull(actual);
  }

  @Disabled("TODO: local use only while Nominatim not self-hosted")
  @Test
  void count_geocoded_address() {
    List<String> addresses =
        List.of(
            "1 Bd du Riou - 06400 Cannes",
            "1 Fbg Saint-Nicolas - 89500 Villeneuve-sur-Yonne",
            "1 Rue de la Loire - 35470 Bain-de-Bretagne",
            "19 Rue du Four Saint-Jacques - Compiègne",
            "2 Av. Jean Jaurès - 03350 Cérilly",
            "2 Rte de Montpellier - 30350 Lédignan",
            "23 Rue de Berlin - 13127 Vitrolles",
            "5 Rue Claude-Marie Perroud - Toulouse",
            "61 Rue de la Chapelle - 75018 Paris",
            "62 Rue Louis Delos - 59709 Marcq-en-Baroeul",
            "D86 Ldit Les Cotes - 07800 Saint-Georges-les-Bains",
            "Pl. de la Gare - Joyeuse",
            "Zoning Industriel Moimont, 11 Rue Jean Jaurès - 95670 Marly-la-Ville");
    var expectedAddresses = expectedAddressesConverted();

    var succeededGeocoding =
        addresses.stream()
            .map(
                s -> {
                  var feature = subject.geocodeAddress(s);
                  try {
                    Thread.sleep(Duration.ofSeconds(1L));
                  } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                  }
                  return feature;
                })
            .filter(Objects::nonNull)
            .toList();

    assertEquals(13, addresses.size());
    assertTrue(
        succeededGeocoding.stream()
            .allMatch(
                feature ->
                    feature.getGeometry() != null
                        && feature.getGeometry().getActualInstance() instanceof Polygon));
    assertEquals(expectedAddresses.size(), succeededGeocoding.size());
    assertTrue(
        expectedAddresses.containsAll(
            succeededGeocoding.stream()
                .map(feature -> feature.getProperties().get("address").toString().replace("\"", ""))
                .toList()));
  }

  private List<String> expectedAddressesConverted() {
    return List.of(
        "1 Bd du Riou - 06400 Cannes",
        "1 Rue de la Loire - 35470 Bain-de-Bretagne",
        "19 Rue du Four Saint-Jacques - Compiègne",
        "2 Av. Jean Jaurès - 03350 Cérilly",
        "2 Rte de Montpellier - 30350 Lédignan",
        "23 Rue de Berlin - 13127 Vitrolles",
        "5 Rue Claude-Marie Perroud - Toulouse",
        "61 Rue de la Chapelle - 75018 Paris",
        "62 Rue Louis Delos - 59709 Marcq-en-Baroeul",
        "Pl. de la Gare - Joyeuse"
        /*Missing are :
         *D86 Ldit Les Cotes - 07800 Saint-Georges-les-Bains
         *Zoning Industriel Moimont, 11 Rue Jean Jaurès - 95670 Marly-la-Ville
         */
        );
  }
}
