package app.bpartners.geojobs.model.geometry.route;

import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_STROKE;
import static app.bpartners.geojobs.model.geometry.route.ObjectType.routeTypeFrom;
import static java.awt.Color.BLACK;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.lang.Math.PI;
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
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import java.awt.*;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

@Disabled
class RoutesContinuationTest {

  PolygonProvider rondPointPolygonProvider =
      new PolygonProvider(
          "/geometry/vgg/rond-point.json", new IntXY(538860, 367567), new IntXY(1024, 1024));
  PolygonProvider rondPointWithPathPolygonProvider =
      new PolygonProvider(
          "/geometry/vgg/line-pathway.json", new IntXY(538860, 367567), new IntXY(1024, 1024));
  PolygonProvider dijonPolygonProvider =
      new PolygonProvider(
          "/geometry/vgg/dijon.json", new IntXY(538860, 367567), new IntXY(1024, 1024));
  PolygonProvider fullParcelPolygonProvider =
      new PolygonProvider(
          "/geometry/vgg/full-parcel.json", new IntXY(538860, 367567), new IntXY(1024, 1024));

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
  void rond_point_continuations_with_details() throws IOException {
    var polygons = rondPointPolygonProvider.getPolygons();
    var scale = 0.1;
    var offset = new IntXY(2_000, 1_000);

    areContinuationsCorrectWithDetails(
        polygons, scale, offset, "/geometry/vgg/rond-point-continuations.png", 0.002);
  }

  @Test
  void rond_point_continued() throws IOException {
    var polygons = rondPointPolygonProvider.getPolygons();
    var scale = 0.1;
    var offset = new IntXY(2_000, 1_000);

    isContinuedCorrect(polygons, scale, offset, "/geometry/vgg/rond-point-continued.png", 0.0005);
  }

  @Test
  void t_like_continuations_with_details() throws IOException {
    var polygons = rondPointPolygonProvider.getPolygons();
    var scale = 0.1;
    var offset = new IntXY(-221_000, 109_000);

    areContinuationsCorrectWithDetails(
        polygons, scale, offset, "/geometry/vgg/t-like-continuations.png", 0.005);
  }

  @Test
  void t_like_point_continued() throws IOException {
    var polygons = rondPointPolygonProvider.getPolygons();
    var scale = 0.1;
    var offset = new IntXY(-221_000, 109_000);

    isContinuedCorrect(polygons, scale, offset, "/geometry/vgg/t-like-continued.png", 0.0005);
  }

  // Who is Lille baro?
  // https://dailyanimeart.com/2015/11/05/nanaos-zanpakuto-kyokotsu-lilles-evolution-bleach-650/lille-barro-grows-head
  @Test
  void lille_barro_continued() throws IOException {
    var polygons = dijonPolygonProvider.getPolygons();
    var scale = 0.1;
    var offset = new IntXY(-170_000, -45_000);

    isContinuedCorrect(polygons, scale, offset, "/geometry/vgg/lille-barro-continued.png", 0.1);
    var continuations =
        areContinuationsCorrectWithDetails(
            polygons, scale, offset, "/geometry/vgg/lille-barro-continuations.png", 0.1);
    assertEquals(47, continuations.continuations().size());
    assertEquals(61, continuations.continued().size());
    assertEquals(324, polygons.size());
  }

  @Test
  void full_parcel_continued() throws IOException {
    var polygons = fullParcelPolygonProvider.getPolygons();
    var plotScale = 0.07;
    var plotOffset = new IntXY(3500, 5500);

    isContinuedCorrect(
        polygons,
        new PrettyConf(50),
        plotScale,
        plotOffset,
        "/geometry/vgg/full-parcel-continued.png",
        0.0005);
  }

  private RoutesContinuation areContinuationsCorrectWithDetails(
      Set<Polygon> polygons,
      double plotScale,
      IntXY plotOffset,
      String expectedImagePath,
      double imageEqualityThreshold)
      throws IOException {
    var alphaConf = alphaConf();
    var continuationConf = continuationConf();
    var unionConf = unionConf();
    var prettyConf = prettyConf();
    var continuations =
        new RoutesContinuation(
            polygons.stream().map(this::toRoute).collect(toSet()),
            new RoutesContinuationConf(alphaConf, unionConf, continuationConf, prettyConf));
    Set<Plotable> plotables =
        continuations.continuations().stream()
            .map(
                p ->
                    new PlotablePolygon(
                        p, new PlotConf(GREEN, new BasicStroke(50), plotScale, plotOffset)))
            .collect(toSet());

    plotables.addAll(
        polygons.stream()
            .map(
                p ->
                    new PlotablePolygon(
                        p, new PlotConf(BLACK, DEFAULT_STROKE, plotScale, plotOffset)))
            .collect(toSet()));
    plotables.addAll(
        continuations.abstractions().stream()
            .flatMap(abstractRoute -> abstractRoute.abstraction().stream())
            .map(oq -> oq.quadrilateral().polygon())
            .map(
                p ->
                    new PlotablePolygon(
                        p, new PlotConf(RED, DEFAULT_STROKE, plotScale, plotOffset)))
            .collect(toSet()));
    var actualImage = new PlotablePlane(1_024, 1_024).plot(plotables);

    var expectedImage = ImageIO.read(this.getClass().getResourceAsStream(expectedImagePath));
    assertTrue(new AreImagesEqual(imageEqualityThreshold).apply(expectedImage, actualImage));

    return continuations;
  }

  private Set<Polygon> isContinuedCorrect(
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
            polygons.stream().map(this::toRoute).collect(toSet()),
            new RoutesContinuationConf(alphaConf, unionConf, continuationConf, prettyConf));
    var continued = continuations.continued();
    Set<Plotable> plotables =
        continued.stream()
            .map(
                p -> new PlotablePolygon(p, new PlotConf(BLACK, new BasicStroke(4), scale, offset)))
            .collect(toSet());

    var actualImage = new PlotablePlane(1_024, 1_024).plot(plotables);

    var expectedImage = ImageIO.read(this.getClass().getResourceAsStream(expectedImagePath));
    assertTrue(new AreImagesEqual(imageEqualityThreshold).apply(expectedImage, actualImage));
    return continued;
  }

  private void isContinuedCorrect(
      Set<Polygon> polygons,
      double scale,
      IntXY offset,
      String expectedImagePath,
      double imageEqualityThreshold)
      throws IOException {
    isContinuedCorrect(
        polygons, prettyConf(), scale, offset, expectedImagePath, imageEqualityThreshold);
  }

  @Test
  void rond_point_with_pathway_continued() throws IOException {
    var polygons = rondPointWithPathPolygonProvider.getPolygons();
    var scale = 0.07;
    var offset = new IntXY(2_000, 1_000);

    isContinuedCorrect(
        polygons,
        new PrettyConf(50),
        scale,
        offset,
        "/geometry/vgg/line-pathway-continued.png",
        0.0005);
  }

  private Route toRoute(Polygon p) {
    Map<String, String> userData = (Map) p.getUserData();
    var label = userData.get("label");
    return new Route(p, routeTypeFrom(label));
  }
}
