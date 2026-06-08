package app.bpartners.geojobs.service.cityjson.texture;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONTexture;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.cityjson.texture.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class CityJsonTextureComputerTest {
  private static final GeometrySquareMeterArea projector = new GeometrySquareMeterArea();
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final CityJsonIOService ioService =
      new CityJsonIOService(new ObjectMapper(), projector);
  private static final CityJsonTextureDomainService domainService =
      new CityJsonTextureDomainService(objectMapper, projector);
  private static final CityJsonTextureComputer subject =
      new CityJsonTextureComputer(ioService, domainService);

  @Test
  void test2() {
    var cityjson = getFile("cityjson/texture/inputs/test2/test2.json");
    var actual = subject.applyTexture(test2Request(), cityjson);
    log.info("CityJSON with texture = {}", actual.getAbsolutePath());

    assertNotSame(actual, cityjson);
  }

  private static CityJSONRequest test2Request() {
    var texture =
        CityJSONTexture.builder()
            .zoom(19)
            .tileX(265541)
            .tileY(180308)
            .imageWidth(3072)
            .imageUri(getFile("cityjson/texture/inputs/test2/test2.jpeg").getAbsolutePath())
            .imageHeight(3072)
            .tileImageSizePx(1024)
            .build();
    return CityJSONRequest.builder().textures(List.of(texture)).build();
  }

  @Test
  void roof7() {
    var cityjson = getFile("cityjson/texture/inputs/roof7/roof7.json");
    var actual = subject.applyTexture(roof7Request(), cityjson);

    assertNotSame(actual, cityjson);
    log.info("CityJSON with texture = {}", actual.getAbsolutePath());
  }

  @Test
  void roof7Roofer() {
    var cityjson = getFile("cityjson/texture/inputs/roof7/roof7_roofer.json");
    var actual = subject.applyTexture(roof7Request(), cityjson);

    assertNotSame(actual, cityjson);
    log.info("CityJSON with texture = {}", actual.getAbsolutePath());
  }

  private static CityJSONRequest roof7Request() {
    var texture =
        CityJSONTexture.builder()
            .zoom(19)
            .tileX(261779)
            .tileY(185145)
            .imageWidth(3072)
            .imageHeight(3072)
            .imageUri(getFile("cityjson/texture/inputs/roof7/roof7.jpg").getAbsolutePath())
            .tileImageSizePx(1024)
            .build();
    return CityJSONRequest.builder().textures(List.of(texture)).build();
  }

  private static File getFile(String resourcePath) {
    try {
      return new File(
          requireNonNull(CityJsonTextureComputer.class.getClassLoader().getResource(resourcePath))
              .toURI());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
