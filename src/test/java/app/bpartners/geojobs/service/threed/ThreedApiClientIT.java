package app.bpartners.geojobs.service.threed;

import static app.bpartners.geojobs.service.threed.model.DelimitationType.ENTIRE_ROOF_DELIMITATION;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.service.threed.conf.ThreedApiProperties;
import app.bpartners.geojobs.service.threed.model.CreateLrgFromFeatureFileUrl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Disabled("Local only")
class ThreedApiClientIT {
  private final String apiUrl = System.getenv("CITY_JSON_PROCESSOR_API_URL");
  private final ThreedApiClient subject =
      new ThreedApiClient(new RestTemplate(), new ThreedApiProperties(apiUrl), new ObjectMapper());

  @Test
  void process() {
    var buildingUrl = "https://dummy.com/file.geojson";

    var actual =
        subject.generate(
            randomUUID().toString(),
            CreateLrgFromFeatureFileUrl.builder()
                .featureFileUrl(buildingUrl)
                .delimitationType(ENTIRE_ROOF_DELIMITATION)
                .build());

    assertNotNull(actual);
    assertNotNull(actual.getFileUrl());
    assertEquals(ENTIRE_ROOF_DELIMITATION, actual.getDelimitationType());
    log.info("url {}", actual.getFileUrl());
  }
}
