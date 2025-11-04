package app.bpartners.geojobs.model.geometry.plot;

import static app.bpartners.geojobs.model.geometry.TestData.compass1Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.croissant1Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.croissant2Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.longPolygon;
import static app.bpartners.geojobs.model.geometry.TestData.quadrilateral1;
import static app.bpartners.geojobs.model.geometry.TestData.quadrilateral2;
import static java.awt.Color.BLACK;
import static java.awt.Color.RED;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PlotablePlaneTest {

  AreImagesEqual areImagesEqual =
      new AreImagesEqual(
          // note(numeric-instability): there are numeric stability problems that occur
          // non-deterministically
          0.005);

  @Test
  void plot_quadrilaterals() throws IOException {
    var expected = ImageIO.read(this.getClass().getResourceAsStream("/geometry/q1-q2.png"));

    var actual =
        new PlotablePlane(1024, 1024)
            .plotQuadrilaterals(Set.of(quadrilateral1(), quadrilateral2()));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void plot_polygon() throws IOException {
    var expected = ImageIO.read(this.getClass().getResourceAsStream("/geometry/long.png"));

    var actual = new PlotablePlane(512, 512).plot(Set.of(new PlotablePolygon(longPolygon(), RED)));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void plot_croissant1() throws IOException {
    var expected = ImageIO.read(this.getClass().getResourceAsStream("/geometry/croissant1.png"));

    var actual =
        new PlotablePlane(512, 512).plot(Set.of(new PlotablePolygon(croissant1Polygon(), BLACK)));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void plot_croissant2() throws IOException {
    var expected = ImageIO.read(this.getClass().getResourceAsStream("/geometry/croissant1.png"));

    var actual =
        new PlotablePlane(512, 512).plot(Set.of(new PlotablePolygon(croissant2Polygon(), BLACK)));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void plot_compass1() throws IOException {
    var expected = ImageIO.read(this.getClass().getResourceAsStream("/geometry/compass1.png"));

    var actual =
        new PlotablePlane(1024, 1024).plot(Set.of(new PlotablePolygon(compass1Polygon(), BLACK)));

    assertTrue(areImagesEqual.apply(expected, actual));
  }
}
