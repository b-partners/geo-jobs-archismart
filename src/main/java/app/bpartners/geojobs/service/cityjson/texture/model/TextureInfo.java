package app.bpartners.geojobs.service.cityjson.texture.model;

import java.io.File;

public record TextureInfo(RasterInfo rasterInfo, File tifFile, String dataUri) {
  public TextureInfo(RasterInfo rasterInfo, File tifFile) {
    this(rasterInfo, tifFile, null);
  }
}
