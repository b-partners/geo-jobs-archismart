package app.bpartners.geojobs.model.geometry.area;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE_IMPORTANTE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE_LEGER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.geometry.area.rate.UsureAreaRateComputer;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import java.util.List;
import org.junit.jupiter.api.Test;

class UsureAreaRateComputerTest extends AreaRateComputerTest {
  @Test
  void compute_are_rate_from_detected_tile() {
    var uLeger = createSquare(1); // Area = 1
    var uImportante = createSquare(2); // Area = 4

    List<DetectedObject> detectedObjects =
        List.of(
            createDetectedObject(uLeger, USURE_LEGER),
            createDetectedObject(uImportante, USURE_IMPORTANTE));

    DetectedTile detectedTile = DetectedTile.builder().detectedObjects(detectedObjects).build();

    double roofArea = 100.0;
    UsureAreaRateComputer computer = new UsureAreaRateComputer(roofArea, detectedTile);

    // USURE_LEGER: malus 1, area 1 -> 1*1 = 1
    // USURE_IMPORTANTE: malus 2, area 4 -> 2*4 = 8
    // Total = (1 + 8) / 100 * 100 = 9.0

    assertEquals(9.0, computer.getUsureAreaRate());
  }

  @Test
  void compute_area_rate_from_polygon_object_types() {
    var uLeger = createSquare(1); // Area = 1
    var uImportante = createSquare(2); // Area = 4

    var polygonObjectTypes =
        List.of(
            new app.bpartners.geojobs.model.geometry.PolygonObjectType(uLeger, USURE_LEGER),
            new app.bpartners.geojobs.model.geometry.PolygonObjectType(
                uImportante, USURE_IMPORTANTE));

    double roofArea = 100.0;
    UsureAreaRateComputer computer = new UsureAreaRateComputer(roofArea, polygonObjectTypes);

    assertEquals(9.0, computer.getUsureAreaRate());
  }
}
