package app.bpartners.geojobs.service.cityjson.texture;

import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.service.cityjson.texture.factory.RasterInfoFactory;
import app.bpartners.geojobs.service.cityjson.texture.model.RasterInfo;
import app.bpartners.geojobs.service.cityjson.texture.model.TextureInfo;
import java.io.File;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CityJsonTextureComputer {
  private final CityJsonIOService cityJsonIOService;
  private final CityJsonTextureDomainService cityJsonTextureDomainService;

  public File applyTexture(CityJSONRequest request, File file) {
    if (request.getTextures() == null || request.getTextures().isEmpty()) {
      return file;
    }

    var texture = request.getTextures().getFirst();
    var info = RasterInfoFactory.make(texture);
    return addTexture(file, info, texture.getImageUri());
  }

  public File addTexture(File cityJsonFile, RasterInfo info, String imageDataUri) {
    var file = cityJsonIOService.loadCityJson(cityJsonFile);
    var cityJsonWithVertices = cityJsonTextureDomainService.toCityJsonWithVertices(file);
    var textureInfo = new TextureInfo(info, imageDataUri);
    var texturedCityJson = cityJsonTextureDomainService.texture(cityJsonWithVertices, textureInfo);
    return cityJsonIOService.toFile(texturedCityJson);
  }
}
