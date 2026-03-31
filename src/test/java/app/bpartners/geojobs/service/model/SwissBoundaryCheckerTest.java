package app.bpartners.geojobs.service.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class SwissBoundaryCheckerTest {
  SwissBoundaryChecker subject = new SwissBoundaryChecker();

  private static Geometry random_coords_outside_switzerland() {
    var roof1Coordinates =
        new Coordinate[] {
          new Coordinate(2.243891733457616, 48.82448842864014),
          new Coordinate(2.243947393505863, 48.82437718542337),
          new Coordinate(2.244038835011281, 48.82440597780899),
          new Coordinate(2.2440209442821413, 48.82445309258651),
          new Coordinate(2.244197863717403, 48.8244975898354),
          new Coordinate(2.24422768160008, 48.82447010624497),
          new Coordinate(2.24432906240051, 48.824487119898066),
          new Coordinate(2.244263463059525, 48.82456695311532),
          new Coordinate(2.243891733457616, 48.82448842864014)
        };
    return geometryFactory.createPolygon(roof1Coordinates);
  }

  public static Geometry switzerland_coords() {
    var swissCoordinates =
        new Coordinate[] {
          new Coordinate(6.13926063513511, 46.183520021503156),
          new Coordinate(6.139108984224631, 46.183499341833546),
          new Coordinate(6.139076241414413, 46.183616526628008),
          new Coordinate(6.139438135632604, 46.18365099274402),
          new Coordinate(6.139551873815464, 46.183178806954572),
          new Coordinate(6.139343353813554, 46.18316157389657),
          new Coordinate(6.13926063513511, 46.183520021503156)
        };
    return geometryFactory.createPolygon(swissCoordinates);
  }

  @Test
  void coordinates_in_switzerland_ok() {
    var actual = subject.isGeometryInSwiss(switzerland_coords());

    assertTrue(actual);
  }

  @Test
  void coordinates_not_in_switzerland_ok() {
    var actual = subject.isGeometryInSwiss(random_coords_outside_switzerland());

    assertFalse(actual);
  }
}
