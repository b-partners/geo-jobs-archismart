package app.bpartners.geojobs.model.geometry.plot;

import static java.awt.Color.BLACK;

import app.bpartners.geojobs.model.geometry.IntXY;
import java.awt.*;

public record PlotConf(Color color, Stroke stroke, double scale, IntXY offset) {
  public static final Color DEFAULT_COLOR = BLACK;
  public static final Stroke DEFAULT_STROKE = new BasicStroke(1);
  public static final int DEFAULT_SCALE = 1;
  public static final IntXY DEFAULT_OFFSET = new IntXY(0, 0);

  public PlotConf() {
    this(DEFAULT_COLOR, DEFAULT_STROKE, DEFAULT_SCALE, DEFAULT_OFFSET);
  }
}
