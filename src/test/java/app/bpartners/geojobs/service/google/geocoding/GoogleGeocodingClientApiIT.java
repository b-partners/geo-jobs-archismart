package app.bpartners.geojobs.service.google.geocoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.service.google.geocoding.api.GeoJsonFeature;
import app.bpartners.geojobs.service.google.geocoding.api.GoogleGeocodingClientApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Disabled("TODO: local use only for now")
class GoogleGeocodingClientApiIT {
  private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();
  GoogleGeocodingClientApi subject =
      new GoogleGeocodingClientApi(new RestTemplate(), System.getenv("GOOGLE_API_KEY"));

  @SneakyThrows
  @Test
  void geocode_address_ok() {
    var actual = subject.findBuildingByAddress("1 Bd du Riou - 06400 Cannes");

    assertTrue(actual.isPresent());
    log.info(om.writeValueAsString(actual.get()));
  }

  @Test
  void count_succeeded_geocoding() {
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

    var actual =
        addresses.stream()
            .map(s -> subject.findBuildingByAddress(s))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

    assertEquals(10, actual.size());
  }

  private File convertGeoFeatureIntoFile(GeoJsonFeature geoJsonFeature) {
    var file = new File(geoJsonFeature.properties().get("address").toString() + ".geojson");
    try {
      Files.write(file.toPath(), om.writeValueAsBytes(geoJsonFeature)).toFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return file;
  }

  @SneakyThrows
  @Test
  void geocode_location() {
    var latitude = 50.630354206820485;
    var longitude = 3.068383335021788;

    var actual = subject.findBuildingByLocation(latitude, longitude);

    assertTrue(actual.isPresent());
    log.info(om.writeValueAsString(actual));
  }
}
