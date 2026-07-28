package app.bpartners.geojobs.service.cityjsonprocessor;

import static app.bpartners.geojobs.service.ciytjsonprocessor.model.DelimitationType.ENTIRE_ROOF_DELIMITATION;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.service.ciytjsonprocessor.CityJsonProcessorApiClient;
import app.bpartners.geojobs.service.ciytjsonprocessor.conf.CityJsonProcessorApiProperties;
import app.bpartners.geojobs.service.ciytjsonprocessor.model.CreateCityJsonFromFeatureFileUrl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Disabled("Local only")
class CityJsonProcessorApicClientIT {
  private final String API_URL = System.getenv("CITY_JSON_PROCESSOR_API_URL");
  private final CityJsonProcessorApiClient subject =
      new CityJsonProcessorApiClient(
          new RestTemplate(), new CityJsonProcessorApiProperties(API_URL), new ObjectMapper());

  @Test
  void process() {
    var buildingUrl = "https://dummy.com/file.geojson";

    var actual =
        subject.generate(
            randomUUID().toString(),
            CreateCityJsonFromFeatureFileUrl.builder()
                .featureFileUrl(buildingUrl)
                .delimitationType(ENTIRE_ROOF_DELIMITATION)
                .build());

    assertNotNull(actual);
    assertNotNull(actual.getFileUrl());
    assertEquals(ENTIRE_ROOF_DELIMITATION, actual.getDelimitationType());
    log.info("url {}", actual.getFileUrl());
  }
}
