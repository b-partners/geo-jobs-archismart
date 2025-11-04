package app.bpartners.geojobs.model;

import static java.lang.Math.abs;

public record ArcgisRasterZoom(int value) {

  private static final double NB_IMAGES_MULTIPLIER_PER_ZOOM_INCREMENT = 4;

  public double nbImagesMultiplierTo(ArcgisRasterZoom that) {
    var diff = value - that.value;
    double multiplier = diff == 0 ? 1 : abs(diff) * NB_IMAGES_MULTIPLIER_PER_ZOOM_INCREMENT;
    return diff < 0 ? 1 / multiplier : multiplier;
  }
}
