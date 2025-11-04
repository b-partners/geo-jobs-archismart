package app.bpartners.geojobs.conf;

import app.bpartners.geojobs.PojaGenerated;
import org.springframework.test.context.DynamicPropertyRegistry;

@PojaGenerated
@SuppressWarnings("all")
public class EmailConf {

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("aws.ses.source", () -> "dummy-ses-source");
  }
}
