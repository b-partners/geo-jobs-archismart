package app.bpartners.geojobs.model.geometry.plot;

import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_OFFSET;
import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_SCALE;
import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_STROKE;
import static java.awt.Color.RED;

import app.bpartners.geojobs.model.geometry.quadrilateral.model.Quadrilateral;
import java.awt.*;

public final class PlotableQuadrilateral extends Plotable {

  private final Quadrilateral q;
  private final PlotConf plotConf;

  public PlotableQuadrilateral(Quadrilateral q, PlotConf plotConf) {
    super(plotConf);
    this.q = q;
    this.plotConf = plotConf;
  }

  public PlotableQuadrilateral(Quadrilateral quadrilateral) {
    this(quadrilateral, new PlotConf(RED, DEFAULT_STROKE, DEFAULT_SCALE, DEFAULT_OFFSET));
  }

  @Override
  public void draw(Graphics2D g2d) {
    new PlotablePolygon(q.polygon(), plotConf).draw(g2d);
  }
}
