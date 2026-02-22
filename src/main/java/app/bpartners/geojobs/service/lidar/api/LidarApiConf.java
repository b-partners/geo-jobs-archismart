package app.bpartners.geojobs.service.lidar.api;

import java.util.Map;
import org.locationtech.jts.geom.Envelope;

public interface LidarApiConf {
  Map<String, String> getDefaultParams(Envelope envelope);

  String getUrl();
}
