package app.bpartners.geojobs.model.geometry.area;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE_CLAIR;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE_INTENSE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import java.util.List;
import org.junit.jupiter.api.Test;

class HumiditeAreaRateComputerTest extends AreaRateComputerTest {

  @Test
  void compute_are_rate_from_detected_tile() {
    var hClair = createSquare(1); // Area = 1
    var hIntense = createSquare(2); // Area = 4

    List<DetectedObject> detectedObjects =
        List.of(
            createDetectedObject(hClair, HUMIDITE_CLAIR),
            createDetectedObject(hIntense, HUMIDITE_INTENSE));

    DetectedTile detectedTile = DetectedTile.builder().detectedObjects(detectedObjects).build();

    double roofArea = 100.0;
    HumiditeAreaRateComputer computer = new HumiditeAreaRateComputer(roofArea, detectedTile);

    // HUMIDITE_CLAIR: malus 1, area 1 -> 1*1 = 1
    // HUMIDITE_INTENSE: malus 2, area 4 -> 2*4 = 8
    // Total = (1 + 8) / 100 * 100 = 9.0

    assertEquals(9.0, computer.getHumidityAreaRate());
    assertEquals(9.0, computer.getGlobalRate()); // weight is 1.0
  }

  @Test
  void compute_area_rate_from_polygon_object_types() {
    var hClair = createSquare(1); // Area = 1
    var hIntense = createSquare(2); // Area = 4

    var polygonObjectTypes =
        List.of(
            new app.bpartners.geojobs.model.geometry.PolygonObjectType(hClair, HUMIDITE_CLAIR),
            new app.bpartners.geojobs.model.geometry.PolygonObjectType(hIntense, HUMIDITE_INTENSE));

    double roofArea = 100.0;
    HumiditeAreaRateComputer computer = new HumiditeAreaRateComputer(roofArea, polygonObjectTypes);

    assertEquals(9.0, computer.getHumidityAreaRate());
  }
}
