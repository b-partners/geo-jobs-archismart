package app.bpartners.geojobs.model.geometry.route;

import static app.bpartners.geojobs.model.geometry.TestData.compass1Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.longPolygon;
import static java.awt.Color.BLACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.PolygonProvider;
import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotConf;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import java.awt.*;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

public class UnifiedRouteTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.00005);
  PolygonProvider lineProvider =
      new PolygonProvider(
          "/geometry/vgg/rond-point.json", new IntXY(538860, 367567), new IntXY(1024, 1024));
  PolygonProvider pathwayProvider =
      new PolygonProvider(
          "/geometry/vgg/pathway.json", new IntXY(538860, 367571), new IntXY(1024, 1024));

  @Test
  void long1_compass1_unified() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/long1-compass1-unified.png"));
    var long1 = longPolygon();
    var compass1 = compass1Polygon();

    var unifiedLine = new UnifiedRoute(Set.of(long1, compass1), new UnionConf(0));
    var actual = new PlotablePlane(1024, 1024).plot(unifiedLine.unified(), BLACK);

    assertEquals(1, unifiedLine.unified().size());
    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void rond_point_is_unified() throws IOException {
    var toUnify = lineProvider.getPolygons();
    assertEquals(89, toUnify.size());

    var unified = new UnifiedRoute(toUnify, new UnionConf(5)).unified();
    var unifiedImage =
        new PlotablePlane(1_024, 1_024)
            .plotPolygons(
                unified, new PlotConf(BLACK, new BasicStroke(1), 0.1, new IntXY(2_000, 1_200)));

    assertEquals(27, unified.size());
    var expectedOutput =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/rond-point-unified.png"));
    assertTrue(areImagesEqual.apply(expectedOutput, unifiedImage));
  }

  @Test
  void pathway_is_unified() throws IOException {
    var toUnify = pathwayProvider.getPolygons();
    assertEquals(8, toUnify.size());

    var unified = new UnifiedRoute(toUnify, new UnionConf(5)).unified();
    var unifiedImage =
        new PlotablePlane(512, 512)
            .plotPolygons(
                unified, new PlotConf(BLACK, new BasicStroke(1), 0.1, new IntXY(2_000, 1_200)));

    assertEquals(6, unified.size());
    var expectedOutput =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/pathway-unified.png"));
    assertTrue(areImagesEqual.apply(expectedOutput, unifiedImage));
  }
}
