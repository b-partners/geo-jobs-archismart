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
  public CityJSONTexture toDomain(ThreeDTextureInfo texture, CityJSONRequest cityJSONRequest) {
    return CityJSONTexture.builder()
        .id(randomUUID().toString())
        .imageUri(texture.getImageDataUri())
        .pixelWidth(texture.getPixelWidth() / 100)
        .pixelHeight(texture.getPixelHeight() / 100)
        .shearY(0.0)
        .shearX(0.0)
        .topLeftLongitude(texture.getTopLeftLon())
        .topLeftLatitude(texture.getTopLeftLat())
        .cityJsonRequest(cityJSONRequest)
        .build();
  }
}
