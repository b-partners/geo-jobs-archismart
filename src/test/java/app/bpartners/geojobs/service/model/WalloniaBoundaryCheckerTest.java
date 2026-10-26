package app.bpartners.geojobs.service.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.model.SwissBoundaryCheckerTest.auvergneRhoneAlpes;
import static app.bpartners.geojobs.service.model.SwissBoundaryCheckerTest.switzerland_coords;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.service.lidar.api.WalloniaBoundaryChecker;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class WalloniaBoundaryCheckerTest {
  WalloniaBoundaryChecker subject = new WalloniaBoundaryChecker();

  public static Geometry liege_coords() {
    var liegeCoordinates =
        new Coordinate[] {
          new Coordinate(5.578864, 50.629200),
          new Coordinate(5.579732, 50.629187),
          new Coordinate(5.579719, 50.628698),
          new Coordinate(5.578851, 50.628711),
          new Coordinate(5.578864, 50.629200)
        };
    return geometryFactory.createPolygon(liegeCoordinates);
  }

  private static Geometry paris_coords() {
    var parisCoordinates =
        new Coordinate[] {
          new Coordinate(2.349014, 48.864716),
          new Coordinate(2.349822, 48.864703),
          new Coordinate(2.349809, 48.864214),
          new Coordinate(2.349001, 48.864227),
          new Coordinate(2.349014, 48.864716)
        };
    return geometryFactory.createPolygon(parisCoordinates);
  }

  @Test
  void coordinates_in_wallonia_ok() {
    var actual = subject.isGeometryInWallonia(liege_coords());

    assertTrue(actual);
  }

  @Test
  void coordinates_not_in_wallonia_ok() {
    var actual = subject.isGeometryInWallonia(paris_coords());

    assertFalse(actual);
  }

  @Test
  void coordinates_in_switzerland_not_in_wallonia_ok() {
    var actual = subject.isGeometryInWallonia(switzerland_coords());

    assertFalse(actual);
  }

  @Test
  void coordinates_outside_wallonia_ok() {
    var actual = subject.isGeometryInWallonia(auvergneRhoneAlpes());

    assertFalse(actual);
  }
}
