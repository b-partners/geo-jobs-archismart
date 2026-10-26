package app.bpartners.geojobs.service.geodata;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class GeodataApiConf {
  private final String url;
  private final String key;

  public GeodataApiConf(
      @Value("${geodata.api.url}") String url, @Value("${geodata.api.key}") String key) {
    this.url = url;
    this.key = key;
  }
}
