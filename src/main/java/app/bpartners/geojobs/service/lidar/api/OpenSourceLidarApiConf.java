package app.bpartners.geojobs.service.lidar.api;

import java.util.HashMap;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSourceLidarApiConf implements LidarApiConf {
  private final String url;

  public OpenSourceLidarApiConf(@Value("${open-source.lidar.api.url}") String url) {
    this.url = url;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public Map<String, String> getDefaultParams(Envelope bbox) {
    var bboxAsString =
        String.format(
            "%s,%s,%s,%s", bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
    var params = new HashMap<String, String>();
    params.put("bbox", bboxAsString);
    return params;
  }
}
