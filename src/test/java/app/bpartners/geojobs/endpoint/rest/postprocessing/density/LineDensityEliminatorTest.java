package app.bpartners.geojobs.endpoint.rest.postprocessing.density;

import static java.lang.Math.PI;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.geometry.PolygonProvider;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.route.ContinuationConf;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import app.bpartners.geojobs.postprocessing.continuer.VGGLinesContinuer;
import app.bpartners.geojobs.postprocessing.density.LineDensityEliminator;
import app.bpartners.geojobs.postprocessing.model.TilingConf;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class LineDensityEliminatorTest {
  PolygonProvider polygonProvider =
      new PolygonProvider("/geometry/vgg/line-with-bad-geometry.json");
  VGGLinesContinuer continuer =
      new VGGLinesContinuer(routesContinuationConf(), new TilingConf(20, 1_024), 10, false);
  private final LineDensityEliminator subject = new LineDensityEliminator();

  @Test
  void remove_lines_with_big_density() throws IOException, URISyntaxException {
    var originalVGG = polygonProvider.getVggAnnotations();

    var cleanedVGG = subject.apply(originalVGG);
    var actual = continuer.apply(cleanedVGG);

    var expectedURI = Paths.get(getClass().getResource("/dijon/line-cleaned.geojson").toURI());
    var expected = Files.readString(expectedURI);

    // actual.saveAsFile("line-cleaned.geojson");
    assertEquals(expected, actual.stringValue());
  }

  private static RoutesContinuationConf routesContinuationConf() {
    var alphaConf = new AlphaConf(0.5, 1);
    var unionConf = new UnionConf(1);
    var continuationConf = new ContinuationConf(PI / 12, PI / 6, 500);
    var prettyConf = new PrettyConf(50);
    return new RoutesContinuationConf(alphaConf, unionConf, continuationConf, prettyConf);
  }
}
