package app.bpartners.geojobs.model.geometry.area;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE_CLAIR;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE_COULEUR;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE_NOIRCIE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.geometry.area.rate.MoisissureAreaRateComputer;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoisissureAreaRateComputerTest extends AreaRateComputerTest {
  @Test
  void compute_are_rate_from_detected_tile() {
    var mNoircie = createSquare(1); // Area = 1
    var mClair = createSquare(2); // Area = 4
    var mCouleur = createSquare(3); // Area = 9

    List<DetectedObject> detectedObjects =
        List.of(
            createDetectedObject(mNoircie, MOISISSURE_NOIRCIE),
            createDetectedObject(mClair, MOISISSURE_CLAIR),
            createDetectedObject(mCouleur, MOISISSURE_COULEUR));

    DetectedTile detectedTile = DetectedTile.builder().detectedObjects(detectedObjects).build();

    double roofArea = 100.0;
    MoisissureAreaRateComputer computer = new MoisissureAreaRateComputer(roofArea, detectedTile);

    // MOISISSURE_NOIRCIE: area 1
    // MOISISSURE_CLAIR: area 4
    // MOISISSURE_COULEUR: area 9
    // Total = (1 + 4 + 9) / 100 * 100 = 14.0

    assertEquals(14.0, computer.getMoisissureAreaRate(), 0.0001);
  }

  @Test
  void compute_area_rate_from_polygon_object_types() {
    var mNoircie = createSquare(1); // Area = 1
    var mClair = createSquare(2); // Area = 4
    var mCouleur = createSquare(3); // Area = 9

    var polygonObjectTypes =
        List.of(
            new app.bpartners.geojobs.model.geometry.PolygonObjectType(
                mNoircie, MOISISSURE_NOIRCIE),
            new app.bpartners.geojobs.model.geometry.PolygonObjectType(mClair, MOISISSURE_CLAIR),
            new app.bpartners.geojobs.model.geometry.PolygonObjectType(
                mCouleur, MOISISSURE_COULEUR));

    double roofArea = 100.0;
    MoisissureAreaRateComputer computer =
        new MoisissureAreaRateComputer(roofArea, polygonObjectTypes);

    assertEquals(14.0, computer.getMoisissureAreaRate(), 0.0001);
  }
}
