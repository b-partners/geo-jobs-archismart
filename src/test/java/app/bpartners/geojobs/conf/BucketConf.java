package app.bpartners.geojobs.conf;

import app.bpartners.geojobs.PojaGenerated;
import org.springframework.test.context.DynamicPropertyRegistry;

@PojaGenerated
@SuppressWarnings("all")
public class BucketConf {

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("aws.s3.bucket", () -> "dummy-bucket");
  }
}
