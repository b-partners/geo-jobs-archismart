package app.bpartners.geojobs.model.geometry.quadrilateral;

import static app.bpartners.geojobs.model.geometry.TestData.compass1Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.croissant1Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.croissant2Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.longPolygon;
import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import app.bpartners.geojobs.model.geometry.plot.PlotablePolygon;
import app.bpartners.geojobs.model.geometry.plot.PlotableQuadrilateral;
import app.bpartners.geojobs.model.geometry.polygon.EnvelopeAsPolygon;
import app.bpartners.geojobs.model.geometry.polygon.LongestInteriorLine;
import app.bpartners.geojobs.model.geometry.polygon.OrientedBoundingBox;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

class SubAlphaTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.005); // note(numeric-instability)

  @Test
  void subAplha_longPolygon() throws IOException {
    var expected = ImageIO.read(this.getClass().getResourceAsStream("/geometry/long-subAlpha.png"));

    var polygon = longPolygon();
    var actual =
        new PlotablePlane(512, 512)
            .plot(
                Set.of(
                    new PlotablePolygon(polygon, BLACK),
                    new PlotableQuadrilateral(new SubAlpha(polygon).get())));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void subAplha_croissant1Polygon() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/croissant1-subAlpha.png"));

    var polygon = croissant1Polygon();
    var actual =
        new PlotablePlane(512, 512)
            .plot(
                Set.of(
                    new PlotablePolygon(polygon, BLACK),
                    new PlotableQuadrilateral(new SubAlpha(polygon).get())));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void subAplha_croissant2Polygon() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/croissant2-subAlpha.png"));

    var polygon = croissant2Polygon();
    var actual =
        new PlotablePlane(512, 512)
            .plot(
                Set.of(
                    new PlotablePolygon(polygon, BLACK),
                    new PlotableQuadrilateral(new SubAlpha(polygon).get())));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void plot_subAlpha_steps_croissant1() throws IOException {
    var expected =
        ImageIO.read(
            this.getClass().getResourceAsStream("/geometry/croissant1-subAlpha-steps.png"));

    var polygon = croissant1Polygon();
    var actual = subAlphaSteps(polygon, 512, 512);

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void plot_subAlpha_steps_longPolygon() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/long-subAlpha-steps.png"));

    var polygon = longPolygon();
    var actual = subAlphaSteps(polygon, 512, 512);

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  private static BufferedImage subAlphaSteps(Polygon polygon, int width, int height) {
    var envelopeOfLil = new EnvelopeAsPolygon(new LongestInteriorLine(polygon).get()).get();
    var obb_of_envelopeOfLil =
        new OrientedBoundingBox((Polygon) envelopeOfLil.intersection(polygon)).get();
    var actual =
        new PlotablePlane(width, height)
            .plot(
                Set.of(
                    new PlotablePolygon(polygon, BLACK),
                    new PlotablePolygon(envelopeOfLil, BLUE),
                    new PlotablePolygon(obb_of_envelopeOfLil, GREEN)));
    return actual;
  }

  @Test
  void plot_subAlpha_compass1() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/compass1-subAlpha.png"));

    var polygon = compass1Polygon();
    var actual =
        new PlotablePlane(1024, 1024)
            .plot(
                Set.of(
                    new PlotablePolygon(polygon, BLACK),
                    new PlotableQuadrilateral(new SubAlpha(polygon).get())));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void plot_subAlpha_steps_compass1() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/compass1-subAlpha-steps.png"));

    var polygon = compass1Polygon();
    var actual = subAlphaSteps(polygon, 1024, 1024);

    assertTrue(areImagesEqual.apply(expected, actual));
  }
}
