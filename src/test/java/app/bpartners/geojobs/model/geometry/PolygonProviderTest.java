package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_OFFSET;
import static java.awt.Color.BLACK;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotConf;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

public class PolygonProviderTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.00005);
  PolygonProvider lineProviderWithOffset =
      new PolygonProvider(
          "/geometry/vgg/rond-point.json", new IntXY(538860, 367567), new IntXY(1024, 1024));
  PolygonProvider pathProvider =
      new PolygonProvider(
          "/geometry/vgg/pathway.json", new IntXY(538860, 367571), new IntXY(1024, 1024));
  PolygonProvider lineAndPathProvider =
      new PolygonProvider(
          "/geometry/vgg/line-pathway.json", new IntXY(538860, 367567), new IntXY(1024, 1024));

  @Test
  void rond_point_is_displayed() throws IOException {
    var toUnify = lineProviderWithOffset.getPolygons();
    var expectedInput =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/rond-point.png"));
    isdDislayCorrect(toUnify, expectedInput, 0.1, DEFAULT_OFFSET);
  }

  @Test
  void t_like_is_displayed() throws IOException {
    var toUnify = lineProviderWithOffset.getPolygons();
    var expectedInput =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/t-like.png"));
    isdDislayCorrect(toUnify, expectedInput, 0.1, new IntXY(-221_000, 109_000));
  }

  private void isdDislayCorrect(
      Set<Polygon> toUnify, BufferedImage expectedInput, double scale, IntXY offset) {
    var toUnifyImage =
        new PlotablePlane(1_024, 1_024)
            .plotPolygons(toUnify, new PlotConf(BLACK, new BasicStroke(1), scale, offset));
    assertTrue(areImagesEqual.apply(expectedInput, toUnifyImage));
  }

  @Test
  void pathway_is_displayed() throws IOException {
    var featuresGeometry = pathProvider.getPolygons();

    var toUnifyImage =
        new PlotablePlane(512, 512)
            .plotPolygons(
                featuresGeometry,
                new PlotConf(BLACK, new BasicStroke(1), 0.1, new IntXY(2_000, 1_200)));

    var expectedInput =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/pathway.png"));
    assertTrue(areImagesEqual.apply(expectedInput, toUnifyImage));
  }

  @Test
  void line_and_pathway_is_displayed() throws IOException {
    var featuresGeometry = lineAndPathProvider.getPolygons();

    var toUnifyImage =
        new PlotablePlane(1024, 1024)
            .plotPolygons(
                featuresGeometry,
                new PlotConf(BLACK, new BasicStroke(1), 0.1, new IntXY(2_000, 1_200)));

    var expectedInput =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/line-pathway.png"));
    assertTrue(areImagesEqual.apply(expectedInput, toUnifyImage));
  }
}
