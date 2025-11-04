package app.bpartners.geojobs.model.geometry.route;

import static app.bpartners.geojobs.model.geometry.route.ObjectType.road;
import static java.awt.Color.BLACK;
import static java.lang.Math.PI;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.PolygonProvider;
import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotConf;
import app.bpartners.geojobs.model.geometry.plot.Plotable;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import app.bpartners.geojobs.model.geometry.plot.PlotablePolygon;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import java.awt.*;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

@Disabled
class DijonRoutesContinuationTest {

  PolygonProvider dijonLine2024PolygonProvider =
      new PolygonProvider(
          "/geometry/vgg/detections_2024_geometrized.json",
          new IntXY(0, 0),
          new IntXY(1024, 1024),
          true);

  private AlphaConf alphaConf() {
    return new AlphaConf(0.5 /*note(alpha-minCoverage)*/, 1);
  }

  private UnionConf unionConf() {
    return new UnionConf(5);
  }

  private ContinuationConf continuationConf() {
    return new ContinuationConf(PI / 12, PI / 6, 500);
  }

  private PrettyConf prettyConf() {
    return new PrettyConf(0);
  }

  @Test
  void dijon_2024_continued() throws IOException {
    var polygons = dijonLine2024PolygonProvider.getPolygons();
    var scale = 0.07;
    var offset = new IntXY(3500, 5500);

    isContinuedCorrect(
        polygons,
        new PrettyConf(50),
        scale,
        offset,
        "/geometry/vgg/full-parcel-continued.png",
        0.0005);
  }

  private void isContinuedCorrect(
      Set<Polygon> polygons,
      PrettyConf prettyConf,
      double scale,
      IntXY offset,
      String expectedImagePath,
      double imageEqualityThreshold)
      throws IOException {
    var alphaConf = alphaConf();
    var continuationConf = continuationConf();
    var unionConf = unionConf();
    var continuations =
        new RoutesContinuation(
            polygons.stream().map(p -> new Route(p, road /*TODO(routeType)*/)).collect(toSet()),
            new RoutesContinuationConf(alphaConf, unionConf, continuationConf, prettyConf));
    Set<Plotable> plotables =
        continuations.continued().stream()
            .map(
                p -> new PlotablePolygon(p, new PlotConf(BLACK, new BasicStroke(4), scale, offset)))
            .collect(toSet());

    var actualImage = new PlotablePlane(1_024, 1_024).plot(plotables);

    var expectedImage = ImageIO.read(this.getClass().getResourceAsStream(expectedImagePath));
    assertTrue(new AreImagesEqual(imageEqualityThreshold).apply(expectedImage, actualImage));
  }
}
