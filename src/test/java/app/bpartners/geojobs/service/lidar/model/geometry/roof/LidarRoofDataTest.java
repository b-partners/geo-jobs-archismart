package app.bpartners.geojobs.service.lidar.model.geometry.roof;

import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.AVAILABLE;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.service.lidar.model.geometry.DelimitedPoints;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class LidarRoofDataTest {
  @Test
  void merge_with_available_ok() {
    var delimitedPoints = DelimitedPoints.builder().points(new HashSet<>()).build();

    var data1 =
        LidarRoofData.builder()
            .roof(delimitedPoints)
            .ground(delimitedPoints)
            .status(AVAILABLE)
            .build();

    var data2 =
        LidarRoofData.builder()
            .roof(delimitedPoints)
            .ground(delimitedPoints)
            .status(UNAVAILABLE)
            .build();

    var expected =
        LidarRoofData.builder()
            .roof(delimitedPoints)
            .ground(delimitedPoints)
            .status(AVAILABLE)
            .build();

    var actual1 = data1.merge(data2);
    var actual2 = data2.merge(data1);

    assertEquals(expected, actual1);
    assertEquals(expected, actual2);
  }
}
