package app.bpartners.geojobs.model.geometry.quadrilateral;

import static app.bpartners.geojobs.model.geometry.TestData.compass1Polygon;
import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_STROKE;
import static app.bpartners.geojobs.model.geometry.route.ObjectType.road;
import static java.awt.Color.BLACK;
import static java.awt.Color.RED;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.PolygonProvider;
import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotConf;
import app.bpartners.geojobs.model.geometry.plot.Plotable;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import app.bpartners.geojobs.model.geometry.plot.PlotablePolygon;
import app.bpartners.geojobs.model.geometry.plot.PlotableQuadrilateral;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.OrientedQuadrilateral;
import app.bpartners.geojobs.model.geometry.route.Route;
import app.bpartners.geojobs.model.geometry.route.UnifiedRoute;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

class AlphaTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.005); // note(numeric-instability)
  PolygonProvider dijonPolygonProvider =
      new PolygonProvider("/geometry/vgg/dijon.json", null, new IntXY(1024, 1024));
  PolygonProvider rondPointPolygonProvider =
      new PolygonProvider(
          "/geometry/vgg/rond-point.json", new IntXY(538860, 367567), new IntXY(1024, 1024));

  @Test
  void vgg1() {
    isAbstractionCorrect(dijonPolygonProvider.apply(1), "/geometry/vgg/vgg1-alpha.png");
  }

  @Test
  void vgg2() {
    isAbstractionCorrect(dijonPolygonProvider.apply(2), "/geometry/vgg/vgg2-alpha-ko.png");
  }

  @Test
  void vgg4() {
    isAbstractionCorrect(dijonPolygonProvider.apply(4), "/geometry/vgg/vgg4-alpha.png");
  }

  @Test
  void vgg5() {
    isAbstractionCorrect(dijonPolygonProvider.apply(5), "/geometry/vgg/vgg5-alpha.png");
  }

  @Test
  void vgg11() {
    isAbstractionCorrect(dijonPolygonProvider.apply(11), "/geometry/vgg/vgg11-alpha.png");
  }

  @Test
  void vgg22() {
    isAbstractionCorrect(
        dijonPolygonProvider.apply(22), "/geometry/vgg/vgg22-alpha-can-be-problematic.png");
  }

  @Test
  void vgg101() {
    isAbstractionCorrect(dijonPolygonProvider.apply(101), "/geometry/vgg/vgg101-alpha.png");
  }

  @Test
  void vgg247() {
    isAbstractionCorrect(dijonPolygonProvider.apply(247), "/geometry/vgg/vgg247-alpha.png");
  }

  @Test
  void abstraction_failures_are_infrequent_enough() {
    var featuresNb = dijonPolygonProvider.featuresNb();
    var failedN = new ArrayList<>();
    for (int n = 0; n < featuresNb; n++) {
      System.out.println(n);
      var alpha = alpha(dijonPolygonProvider.apply(n));
      if (alpha.get().isEmpty()) {
        failedN.add(n);
      }
    }

    assertEquals(324, featuresNb);
    assertEquals(84, failedN.size());
  }

  @Test
  void rond_point_is_correctly_abstracted() throws IOException {
    var polygons = rondPointPolygonProvider.getPolygons();
    var plotScale = 0.1;
    var plotOffset = new IntXY(2_000, 1_000);

    Set<Plotable> plotablesQ = new HashSet<>();
    for (var p : polygons) {
      var conf =
          new AlphaConf(
              // note(alpha-minCoverage)
              // If greater minCoverageOfAbstractedArea is used
              // then will result in many subAlpha failures,
              // which in turn will counter-intuitively result in less quadrilaterals abstracted.
              // Indeed, we purposefully make the whole alpha fail if at least 1 subAlpha fails.
              0.5, 1);
      var oqList = new Alpha(new Route(p, road /*TODO(routeType)*/), conf).get();
      var qPlotConf = new PlotConf(RED, DEFAULT_STROKE, plotScale, plotOffset);
      plotablesQ.addAll(
          oqList.stream()
              .map(oq -> new PlotableQuadrilateral(oq.quadrilateral(), qPlotConf))
              .collect(toSet()));
    }
    Set<Plotable> plotables = new HashSet<>(plotablesQ);

    var unified = new UnifiedRoute(polygons, new UnionConf(5)).unified().stream().toList();
    var pPlotConf = new PlotConf(BLACK, DEFAULT_STROKE, plotScale, plotOffset);
    plotables.addAll(
        unified.stream().map(polygon -> new PlotablePolygon(polygon, pPlotConf)).collect(toSet()));
    var image = new PlotablePlane(1_024, 1_024).plot(plotables);
    var expectedOutput =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/rond-point-alpha.png"));
    assertTrue(areImagesEqual.apply(expectedOutput, image));
  }

  @Test
  void compass1() {
    isAbstractionCorrect(compass1Polygon(), "/geometry/compass1-alpha.png");
  }

  private void isAbstractionCorrect(Polygon polygon, String pathOfExpectedImage) {
    BufferedImage expected = readImage(pathOfExpectedImage);

    Set<Plotable> plotables = new HashSet<>();
    plotables.add(new PlotablePolygon(polygon, BLACK));
    var alpha = alpha(polygon);
    plotables.addAll(
        alpha.get().stream()
            .map(OrientedQuadrilateral::quadrilateral)
            .map(PlotableQuadrilateral::new)
            .collect(toSet()));
    var actual = new PlotablePlane(1024, 1024).plot(plotables);

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  private static Alpha alpha(Polygon p) {
    return new Alpha(new Route(p, road /*TODO(routeType)*/), new AlphaConf(0.95, 100));
  }

  @SneakyThrows
  private BufferedImage readImage(String path) {
    return ImageIO.read(this.getClass().getResourceAsStream(path));
  }
}
