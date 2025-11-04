package app.bpartners.geojobs.endpoint.rest.postprocessing;

public record MergeConf(double minAreaThreshold, double iouAllowed) {
  public static MergeConf getInstance(int minAreaThreshold) {
    return new MergeConf(minAreaThreshold, 0.6);
  }
}
