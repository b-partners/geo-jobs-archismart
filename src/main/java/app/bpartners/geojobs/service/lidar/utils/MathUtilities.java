package app.bpartners.geojobs.service.lidar.utils;

public class MathUtilities {
  private MathUtilities() {}

  public static double ceil2(double value) {
    return Math.ceil(value * 100) / 100.0;
  }

  public static double round2(double value) {
    return Math.round(value * 100) / 100.0;
  }
}
