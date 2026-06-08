package app.bpartners.geojobs.service.cityjson.texture.factory;

import app.bpartners.geojobs.repository.model.cityjson.CityJSONTexture;
import app.bpartners.geojobs.service.cityjson.texture.model.RasterInfo;

public class RasterInfoFactory {
  private RasterInfoFactory() {}

  public static RasterInfo make(CityJSONTexture texture) {
    return RasterInfo.builder()
        .zoom(texture.getZoom())
        .tileX(texture.getTileX())
        .tileY(texture.getTileY())
        .width(texture.getImageWidth())
        .height(texture.getImageHeight())
        .tileImageSizePx(texture.getTileImageSizePx())
        .build();
  }
}
