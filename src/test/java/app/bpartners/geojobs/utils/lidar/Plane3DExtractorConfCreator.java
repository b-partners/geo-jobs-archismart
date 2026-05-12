package app.bpartners.geojobs.utils.lidar;

import app.bpartners.geojobs.model.lidar.planes.PlaneDelimitation.PlaneDelimitationConf;
import app.bpartners.geojobs.model.lidar.planes.conf.RangedConf;

public class Plane3DExtractorConfCreator {
  public static PlaneDelimitationConf planeDelimitationConf() {
    return PlaneDelimitationConf.builder()
        .concaveRatio(
            RangedConf.from(
                new RangedConf.IntegerRangedConf<>(Integer.MIN_VALUE, 200, 0.2),
                new RangedConf.IntegerRangedConf<>(201, Integer.MAX_VALUE, 0.2)))
        .dpsEpsilon(0.5)
        .lrDegEpsilon(5)
        .build();
  }
}
