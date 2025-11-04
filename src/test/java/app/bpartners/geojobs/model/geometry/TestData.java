package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.model.geometry.route.ObjectType.road;
import static java.lang.Math.PI;
import static org.locationtech.jts.geom.util.AffineTransformation.rotationInstance;
import static org.locationtech.jts.geom.util.AffineTransformation.translationInstance;

import app.bpartners.geojobs.model.geometry.quadrilateral.model.Quadrilateral;
import app.bpartners.geojobs.model.geometry.route.Route;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

public class TestData {

  private static final GeometryFactory geometryFactory = new GeometryFactory();

  public static Quadrilateral quadrilateral1() {
    return new Quadrilateral(
        Set.of(
            new Coordinate(10, 10),
            new Coordinate(300, 100),
            new Coordinate(100, 200),
            new Coordinate(400, 400)));
  }

  public static Quadrilateral quadrilateral2() {
    return new Quadrilateral(
        Set.of(
            new Coordinate(500, 510),
            new Coordinate(400, 700),
            new Coordinate(910, 1000),
            new Coordinate(800, 690)));
  }

  public static Polygon longPolygon() {
    return geometryFactory.createPolygon(
        geometryFactory.createLinearRing(
            new Coordinate[] {
              new Coordinate(40, 40),
              new Coordinate(60, 60),
              new Coordinate(100, 30),
              new Coordinate(200, 400),
              new Coordinate(200, 500),
              new Coordinate(40, 40) // Close the ring
            }));
  }

  public static Polygon croissant1Polygon() {
    var unrotated =
        geometryFactory.createPolygon(
            geometryFactory.createLinearRing(
                new Coordinate[] {
                  new Coordinate(140, 300),
                  new Coordinate(120, 200),
                  new Coordinate(150, 110),
                  new Coordinate(360, 120),
                  new Coordinate(450, 140),
                  new Coordinate(350, 350),
                  new Coordinate(300, 350),
                  new Coordinate(380, 190),
                  new Coordinate(190, 190),
                  new Coordinate(140, 300)
                }));
    return rotate(unrotated, PI / 4, 200, 200);
  }

  public static Polygon croissant2Polygon() {
    var unrotated =
        geometryFactory.createPolygon(
            geometryFactory.createLinearRing(
                new Coordinate[] {
                  new Coordinate(140, 300),
                  new Coordinate(120, 200),
                  new Coordinate(150, 110),
                  new Coordinate(360, 120),
                  new Coordinate(450, 140),
                  new Coordinate(350, 350),
                  new Coordinate(300, 350),
                  new Coordinate(380, 190),
                  new Coordinate(160, 160),
                  new Coordinate(140, 300)
                }));
    return rotate(unrotated, PI / 4, 200, 200);
  }

  public static Polygon compass1Polygon() {
    var unrotated =
        geometryFactory.createPolygon(
            geometryFactory.createLinearRing(
                new Coordinate[] {
                  new Coordinate(30, 250),
                  new Coordinate(40, 150),
                  new Coordinate(70, 150),
                  new Coordinate(400, 10),
                  new Coordinate(550, 25),
                  new Coordinate(70, 180),
                  new Coordinate(70, 260),
                  new Coordinate(650, 300),
                  new Coordinate(550, 350),
                  new Coordinate(60, 320),
                  new Coordinate(50, 300),
                  new Coordinate(30, 250)
                }));

    return rotate(unrotated, PI / 8, 0, 500);
  }

  private static Polygon rotate(Polygon p, double theta, double x, double y) {
    var rotation = rotationInstance(theta, x, y);
    return (Polygon) rotation.transform(p);
  }

  private static Polygon translate(Polygon p, double x, double y) {
    var translation = translationInstance(x, y);
    return (Polygon) translation.transform(p);
  }

  public static Route compass2Route() {
    return new Route(translate(compass1Polygon(), -50, -100), road /*TODO(routeType)*/);
  }

  public static Route long2Route() {
    return new Route(
        rotate(translate(longPolygon(), 500, 550), -PI / 30, 0, 0), road /*TODO(routeType)*/);
  }
}
