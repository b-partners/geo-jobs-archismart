package app.bpartners.geojobs.service.cityjson.texture;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONTexture;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.cityjson.texture.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class CityJsonTextureComputerTest {

  GeometrySquareMeterArea geometrySquareMeterArea = new GeometrySquareMeterArea();
  ObjectMapper objectMapper = new ObjectMapper();
  CityJsonIOService cityJsonIOService =
      new CityJsonIOService(new ObjectMapper(), geometrySquareMeterArea);
  CityJsonTextureDomainService cityJsonTextureDomainService =
      new CityJsonTextureDomainService(objectMapper, geometrySquareMeterArea);
  CityJsonTextureComputer subject =
      new CityJsonTextureComputer(
          cityJsonIOService, cityJsonTextureDomainService, geometrySquareMeterArea);

  @Test
  void texturize_from_cityjson_request() throws IOException {
    testRoof(2);
    testRoof(4);
    testRoof(5);
  }

  private void testRoof(int roofNumber) throws IOException {
    Payload payload = getPayload(roofNumber);

    var actual = subject.applyTexture(payload.cityJSONRequest(), payload.cityJsonFile());

    assertNotNull(actual);

    com.fasterxml.jackson.databind.node.ObjectNode resultJson =
        cityJsonIOService.loadCityJson(actual);
    assertNotNull(resultJson.get("appearance"));
    com.fasterxml.jackson.databind.node.ObjectNode appearance =
        (com.fasterxml.jackson.databind.node.ObjectNode) resultJson.get("appearance");
    assertTrue(appearance.has("textures"));
    assertTrue(appearance.has("materials"));
    assertTrue(appearance.has("vertices-texture"));

    assertEquals(
        payload.imageFile().getAbsolutePath(),
        appearance.get("textures").get(0).get("image").asText());

    com.fasterxml.jackson.databind.node.ObjectNode cityObjects =
        (com.fasterxml.jackson.databind.node.ObjectNode) resultJson.get("CityObjects");
    cityObjects
        .fields()
        .forEachRemaining(
            entry -> {
              com.fasterxml.jackson.databind.JsonNode cityObject = entry.getValue();
              if ("Building".equals(cityObject.get("type").asText())) {
                com.fasterxml.jackson.databind.node.ArrayNode geometries =
                    (com.fasterxml.jackson.databind.node.ArrayNode) cityObject.get("geometry");
                for (com.fasterxml.jackson.databind.JsonNode geometry : geometries) {
                  assertTrue(geometry.has("appearance"));
                  com.fasterxml.jackson.databind.JsonNode geomAppearance =
                      geometry.get("appearance");
                  assertTrue(geomAppearance.has("texture"));
                  assertTrue(geomAppearance.has("material"));
                }
              }
            });
  }

  private @NotNull Payload getPayload(int roofNumber) throws IOException {
    File cityJsonFile =
        new ClassPathResource(
                String.format("cityjson/texture/inputs/roof%s/roof%s.json", roofNumber, roofNumber))
            .getFile();
    File tifFile =
        new ClassPathResource(
                String.format("cityjson/texture/inputs/roof%s/roof%s.tif", roofNumber, roofNumber))
            .getFile();
    TextureInfo textureInfo = cityJsonIOService.loadTexture(tifFile);
    double pixelWidth = textureInfo.rasterInfo().pixelWidth();
    double pixelHeight = textureInfo.rasterInfo().pixelHeight();
    double originX = textureInfo.rasterInfo().originX();
    double originY = textureInfo.rasterInfo().originY();

    List<org.locationtech.jts.math.Vector3D> projectedOrigin =
        cityJsonTextureDomainService.project(
            List.of(new org.locationtech.jts.math.Vector3D(originX, originY, 0)),
            LAMBERT_93,
            WGS84);
    double lon = projectedOrigin.get(0).getX();
    double lat = projectedOrigin.get(0).getY();

    CityJSONRequest cityJSONRequest = CityJSONRequest.builder().id(randomUUID().toString()).build();
    File imageFile =
        new ClassPathResource(
                String.format("cityjson/texture/outputs/roof%s/texture.png", roofNumber))
            .getFile();
    CityJSONTexture texture =
        CityJSONTexture.builder()
            .id(randomUUID().toString())
            .imageUri(imageFile.getAbsolutePath())
            .topLeftLongitude(lon)
            .topLeftLatitude(lat)
            .pixelWidth(pixelWidth)
            .pixelHeight(pixelHeight)
            .imageWidth(textureInfo.rasterInfo().width())
            .imageHeight(textureInfo.rasterInfo().height())
            .cityJsonRequest(cityJSONRequest)
            .build();
    cityJSONRequest.setTextures(List.of(texture));
    return new Payload(cityJsonFile, cityJSONRequest, imageFile);
  }

  private record Payload(File cityJsonFile, CityJSONRequest cityJSONRequest, File imageFile) {}

  @Test
  void texturize_from_raster_info() throws IOException {
    int roofNumber = 4;
    File cityJsonFile =
        new ClassPathResource(
                String.format("cityjson/texture/inputs/roof%s/roof%s.json", roofNumber, roofNumber))
            .getFile();
    File tifFile =
        new ClassPathResource(
                String.format("cityjson/texture/inputs/roof%s/roof%s.tif", roofNumber, roofNumber))
            .getFile();
    RasterInfo rasterInfo = cityJsonIOService.readRasterInfo(tifFile);
    String imageDataUri = "mockImageDataUri";

    var actual = subject.textureCityJson(cityJsonFile, rasterInfo, imageDataUri);

    assertNotNull(actual);
  }

  @Test
  void uv_edge_mapping() {
    RasterInfo rasterInfo =
        new RasterInfo(
            100.0, 200.0, // origin
            0.5, -0.5, // pixel size
            0.0, 0.0, // shear
            100, 100, // width, height (so 50x50 area)
            WGS84);

    // Top-left corner (origin)
    List<org.locationtech.jts.math.Vector3D> topLeft =
        List.of(new org.locationtech.jts.math.Vector3D(100.0, 200.0, 0));
    List<UV> uvTopLeft = cityJsonTextureDomainService.computeUv(topLeft, rasterInfo);
    assertEquals(0.0, uvTopLeft.get(0).u(), 1e-6, "Top-left U should be 0");
    assertEquals(1.0, uvTopLeft.get(0).v(), 1e-6, "Top-left V should be 1");

    // Bottom-right corner
    // x = 100 + 100 * 0.5 = 150
    // y = 200 + 100 * -0.5 = 150
    List<org.locationtech.jts.math.Vector3D> bottomRight =
        List.of(new org.locationtech.jts.math.Vector3D(150.0, 150.0, 0));
    List<UV> uvBottomRight = cityJsonTextureDomainService.computeUv(bottomRight, rasterInfo);
    assertEquals(1.0, uvBottomRight.get(0).u(), 1e-6, "Bottom-right U should be 1");
    assertEquals(0.0, uvBottomRight.get(0).v(), 1e-6, "Bottom-right V should be 0");

    // A point in the middle
    // x = 125, y = 175
    List<org.locationtech.jts.math.Vector3D> middle =
        List.of(new org.locationtech.jts.math.Vector3D(125.0, 175.0, 0));
    List<UV> uvMiddle = cityJsonTextureDomainService.computeUv(middle, rasterInfo);
    assertEquals(0.5, uvMiddle.get(0).u(), 1e-6, "Middle U should be 0.5");
    assertEquals(0.5, uvMiddle.get(0).v(), 1e-6, "Middle V should be 0.5");

    // Sub-pixel point
    // x = 100 + 0.5 * 0.5 = 100.25
    // y = 200 + 0.5 * -0.5 = 199.75
    List<org.locationtech.jts.math.Vector3D> subPixel =
        List.of(new org.locationtech.jts.math.Vector3D(100.125, 199.875, 0));
    List<UV> uvSubPixel = cityJsonTextureDomainService.computeUv(subPixel, rasterInfo);
    // col = (100.125 - 100) / 0.5 = 0.25
    // row = (199.875 - 200) / -0.5 = 0.25
    // u = 0.25 / 100 = 0.0025
    // v = 1.0 - (0.25 / 100) = 0.9975
    assertEquals(0.0025, uvSubPixel.get(0).u(), 1e-6, "Sub-pixel U should be 0.0025");
    assertEquals(0.9975, uvSubPixel.get(0).v(), 1e-6, "Sub-pixel V should be 0.9975");
  }
}
