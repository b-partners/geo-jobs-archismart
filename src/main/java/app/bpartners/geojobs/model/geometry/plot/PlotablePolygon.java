package app.bpartners.geojobs.model.geometry.plot;

import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_OFFSET;
import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_SCALE;
import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_STROKE;

import java.awt.*;
import java.util.Arrays;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

public final class PlotablePolygon extends Plotable {

  private final Polygon polygon;

  public PlotablePolygon(Polygon polygon, PlotConf plotConf) {
    super(plotConf);
    this.polygon = polygon;
  }

  public PlotablePolygon(Polygon polygon, Color color) {
    this(polygon, new PlotConf(color, DEFAULT_STROKE, DEFAULT_SCALE, DEFAULT_OFFSET));
  }

  @Override
  public void draw(Graphics2D g2d) {
    draw(g2d, polygon.getExteriorRing().getCoordinates(), plotConf);
    for (int n = 0; n < polygon.getNumInteriorRing(); n++) {
      draw(g2d, polygon.getInteriorRingN(n).getCoordinates(), plotConf);
    }
  }

  private static void draw(Graphics2D g2d, Coordinate[] coordinates, PlotConf plotConf) {
    var scale = plotConf.scale();
    var offset = plotConf.offset();
    g2d.drawPolygon(
        Arrays.stream(coordinates).mapToInt(c -> (int) ((c.x + offset.x()) * scale)).toArray(),
        Arrays.stream(coordinates).mapToInt(c -> (int) ((c.y + offset.y()) * scale)).toArray(),
        coordinates.length);
  }
}
