package app.bpartners.geojobs.endpoint.rest.postprocessing.model;

public record TilingConf(int z, int imgSize) {
  public static TilingConf getDefaultInstance() {
    return new TilingConf(20, 1024);
  }
}
