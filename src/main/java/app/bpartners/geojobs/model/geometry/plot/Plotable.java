package app.bpartners.geojobs.model.geometry.plot;

import java.awt.*;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract sealed class Plotable permits PlotableQuadrilateral, PlotablePolygon {

  protected final PlotConf plotConf;

  public final void plot(Graphics2D g2d) {
    g2d.setColor(plotConf.color());
    g2d.setStroke(plotConf.stroke());
    draw(g2d);
  }

  protected abstract void draw(Graphics2D g2d);
}
