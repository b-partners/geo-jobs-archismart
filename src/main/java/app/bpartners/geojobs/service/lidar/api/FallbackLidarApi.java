package app.bpartners.geojobs.service.lidar.api;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Envelope;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FallbackLidarApi {
  private static final String NEW_DATA_URL_FOR_DEPRECATED_DTA_FORMAT =
      "https://data.geopf.fr/telechargement/download/LiDARHD-NUALID/NUALHD_1-0__LAZ_LAMB93_KA_2025-07-22/LHD_FXX_%04d_%04d_PTS_LAMB93_IGN69.copc.laz";

  private static Set<String> getLidarTilesFromBBOX(
      double minX, double minY, double maxX, double maxY, String format) {
    int txMin = (int) Math.floor(minX / 1000.0);
    int txMax = (int) Math.floor(maxX / 1000.0);
    int tyMin = (int) Math.floor(minY / 1000.0);
    int tyMax = (int) Math.floor(maxY / 1000.0);

    Set<String> tiles = new HashSet<>();
    for (int tx = txMin; tx <= txMax; tx++) {
      for (int ty = tyMin; ty <= tyMax; ty++) {
        tiles.add(String.format(format, tx, ty + 1));
      }
    }
    return tiles;
  }

  private static Set<String> getLidarTilesForDeprecatedData(
      double minX, double minY, double maxX, double maxY) {
    return getLidarTilesFromBBOX(minX, minY, maxX, maxY, NEW_DATA_URL_FOR_DEPRECATED_DTA_FORMAT);
  }

  public Set<String> getUniqueLidarUrlsForDeprecatedData(Envelope envelope) {
    log.info("Using fallback URL because files are deprecated on WFS");
    return getLidarTilesForDeprecatedData(
        envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
  }
}
