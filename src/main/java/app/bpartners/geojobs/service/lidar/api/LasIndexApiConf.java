package app.bpartners.geojobs.service.lidar.api;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Slf4j
@Configuration
public class LasIndexApiConf {
  private final String url;
  private static final String LAS_INDEX_PATH = "/lidar/index";

  public LasIndexApiConf(@Value("${lidar.index.api.url}") String url) {
    this.url = url;
  }

  public String getLasIndexApiUrl() {
    return url + LAS_INDEX_PATH;
  }
}
