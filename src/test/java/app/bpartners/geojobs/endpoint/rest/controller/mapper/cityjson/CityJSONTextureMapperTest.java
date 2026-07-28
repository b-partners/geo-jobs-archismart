package app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import app.bpartners.geojobs.endpoint.rest.model.ThreeDTextureInfo;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONTexture;
import org.junit.jupiter.api.Test;

class CityJSONTextureMapperTest {
  CityJSONTextureMapper subject = new CityJSONTextureMapper();

  @Test
  void to_domain_ok() {
    var request = CityJSONRequest.builder().id("requestId").build();
    var texture =
        new ThreeDTextureInfo()
            .imageUri("imageUri")
            .imageWidth(10)
            .imageHeight(20)
            .zoom(19)
            .tileX(1)
            .tileY(2)
            .tileImageSizePx(256);

    var actual = subject.toDomain(texture, request);

    assertNotNull(actual.getId());
    assertEquals("imageUri", actual.getImageUri());
    assertEquals(10, actual.getImageWidth());
    assertEquals(20, actual.getImageHeight());
    assertEquals(19, actual.getZoom());
    assertEquals(1, actual.getTileX());
    assertEquals(2, actual.getTileY());
    assertEquals(256, actual.getTileImageSizePx());
    assertSame(request, actual.getCityJsonRequest());
  }

  @Test
  void to_rest_ok() {
    var texture =
        CityJSONTexture.builder()
            .id("id")
            .imageUri("imageUri")
            .imageWidth(10)
            .imageHeight(20)
            .zoom(19)
            .tileX(1)
            .tileY(2)
            .tileImageSizePx(256)
            .build();

    var actual = subject.toRest(texture);

    assertEquals(
        new ThreeDTextureInfo()
            .imageUri("imageUri")
            .imageWidth(10)
            .imageHeight(20)
            .zoom(19)
            .tileX(1)
            .tileY(2)
            .tileImageSizePx(256),
        actual);
  }
}
