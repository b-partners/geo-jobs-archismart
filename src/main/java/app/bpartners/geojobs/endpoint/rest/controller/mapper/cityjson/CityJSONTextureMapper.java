package app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.ThreeDTextureInfo;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONTexture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CityJSONTextureMapper {
  public CityJSONTexture toDomain(ThreeDTextureInfo texture, CityJSONRequest request) {
    return CityJSONTexture.builder()
        .id(randomUUID().toString())
        .imageUri(texture.getImageUri())
        .imageWidth(texture.getImageWidth())
        .imageHeight(texture.getImageHeight())
        .cityJsonRequest(request)
        .zoom(texture.getZoom())
        .tileX(texture.getTileX())
        .tileY(texture.getTileY())
        .tileImageSizePx(texture.getTileImageSizePx())
        .build();
  }
}
