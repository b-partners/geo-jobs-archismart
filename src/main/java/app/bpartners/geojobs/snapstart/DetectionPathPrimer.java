package app.bpartners.geojobs.snapstart;

import static java.awt.Color.RED;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static javax.imageio.ImageIO.write;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.FilePolygonDrawer;
import app.bpartners.geojobs.service.TileImagesAssembler;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import app.bpartners.geojobs.service.detection.DetectionPayloadV2;
import app.bpartners.geojobs.service.detection.DetectionResponseV2;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class DetectionPathPrimer implements Resource {
  private static final int TILE_SIZE = 1024;
  private static final int TILE_X = 523561;
  private static final int TILE_Y = 370292;
  private static final int TILE_ZOOM = 20;

  private final DetectionMaskCreator maskCreator;
  private final FilePolygonDrawer polygonDrawer;
  private final TileImagesAssembler imagesAssembler;
  private final GeometryConverter geometryConverter;
  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;

  public DetectionPathPrimer(
      DetectionMaskCreator maskCreator,
      FilePolygonDrawer polygonDrawer,
      TileImagesAssembler imagesAssembler,
      GeometryConverter geometryConverter,
      ObjectMapper objectMapper,
      RestTemplate restTemplate) {
    this.maskCreator = maskCreator;
    this.polygonDrawer = polygonDrawer;
    this.imagesAssembler = imagesAssembler;
    this.geometryConverter = geometryConverter;
    this.objectMapper = objectMapper;
    this.restTemplate = restTemplate;
    Core.getGlobalContext().register(this);
  }

  @Override
  public void beforeCheckpoint(Context<? extends Resource> context) {
    prime();
  }

  @Override
  public void afterRestore(Context<? extends Resource> context) {
    log.info("Restored from a snapshot holding an already primed detection path");
  }

  public boolean prime() {
    var start = now();
    var temporaryFiles = new ArrayList<File>();
    try {
      var tileImage = writeSyntheticTileImage();
      temporaryFiles.add(tileImage);
      temporaryFiles.add(primeMaskDrawing());
      temporaryFiles.add(primePolygonDrawing(tileImage));
      temporaryFiles.add(primeImageAssembling(tileImage));
      primeGeometryConversion();
      primeJsonMapping(tileImage);
      primeDetectionApiCall();
      log.info("Detection path primed in {} ms", Duration.between(start, now()).toMillis());
      return true;
    } catch (Exception | LinkageError e) {
      log.warn(
          "Could not prime the detection path, the first detection of each container will stay"
              + " slow: {}",
          e.getMessage());
      return false;
    } finally {
      temporaryFiles.forEach(DetectionPathPrimer::deleteQuietly);
    }
  }

  private File writeSyntheticTileImage() throws Exception {
    var image = new BufferedImage(TILE_SIZE, TILE_SIZE, TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(RED);
    graphics.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
    graphics.dispose();
    var file = File.createTempFile("primer_tile_" + randomUUID(), ".jpg");
    write(image, "jpg", file);
    return file;
  }

  private File primeMaskDrawing() {
    return maskCreator.apply(
        List.of(
            List.of(BigDecimal.valueOf(10), BigDecimal.valueOf(10)),
            List.of(BigDecimal.valueOf(900), BigDecimal.valueOf(40)),
            List.of(BigDecimal.valueOf(500), BigDecimal.valueOf(800))));
  }

  private File primePolygonDrawing(File tileImage) {
    return polygonDrawer.apply(
        List.of(List.of(List.of(new IntXY(10, 10), new IntXY(900, 40), new IntXY(500, 800)))),
        RED,
        tileImage);
  }

  private File primeImageAssembling(File tileImage) {
    return imagesAssembler.apply(
        List.of(tileAt(TILE_X, TILE_Y, tileImage), tileAt(TILE_X, TILE_Y + 1, tileImage)));
  }

  private Tile tileAt(int x, int y, File image) {
    return Tile.builder()
        .id(randomUUID().toString())
        .coordinates(new TileCoordinates().x(x).y(y).z(TILE_ZOOM))
        .image(image)
        .build();
  }

  private void primeGeometryConversion() {
    var tileMultiPolygon = geometryConverter.getMultiPolygonFromTile(TILE_X, TILE_Y, TILE_ZOOM);
    var geometryAsString = geometryConverter.writeGeometryAsString(tileMultiPolygon);
    var readBackGeometry = geometryConverter.readGeometryFromString(geometryAsString);
    readBackGeometry.intersection(tileMultiPolygon);
    geometryConverter.geometryToMultiPolygonCoordinates(readBackGeometry);
  }

  private void primeJsonMapping(File tileImage) throws Exception {
    var feature =
        objectMapper.readValue(
            """
            {
              "type": "Feature",
              "properties": {"zoom": 20},
              "geometry": {
                "type": "Polygon",
                "coordinates": [[[-0.2494458, 46.65203], [-0.2494733, 46.6519681],
                                 [-0.2494903, 46.6519052], [-0.2494458, 46.65203]]]
              }
            }
            """,
            app.bpartners.geojobs.endpoint.rest.model.Feature.class);
    objectMapper.writeValueAsString(feature);

    var base64ImgData = Base64.getEncoder().encodeToString(readFileToByteArray(tileImage));
    objectMapper.writeValueAsString(
        DetectionPayloadV2.builder()
            .fileName(tileImage.getName())
            .base64ImgData(base64ImgData)
            .build());
    objectMapper.readValue(
        objectMapper.writeValueAsString(sampleDetectionResponse()), DetectionResponseV2.class);
  }

  private void primeDetectionApiCall() throws Exception {
    var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/",
        exchange -> {
          var body = objectMapper.writeValueAsBytes(sampleDetectionResponse());
          exchange.getResponseHeaders().add("Content-Type", APPLICATION_JSON.toString());
          exchange.sendResponseHeaders(200, body.length);
          try (var responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
          }
        });
    server.start();
    try {
      var headers = new HttpHeaders();
      headers.setContentType(APPLICATION_JSON);
      var request =
          new HttpEntity<>(objectMapper.writeValueAsString(sampleDetectionResponse()), headers);
      restTemplate.postForEntity(stubUrlOf(server), request, DetectionResponseV2.class);
    } finally {
      server.stop(0);
    }
  }

  private static String stubUrlOf(HttpServer server) {
    return "http://"
        + server.getAddress().getAddress().getHostAddress()
        + ":"
        + server.getAddress().getPort()
        + "/";
  }

  private DetectionResponseV2 sampleDetectionResponse() {
    Map<String, DetectionResponseV2.ImageData.Region> regions = new HashMap<>();
    regions.put(
        "region_1",
        DetectionResponseV2.ImageData.Region.builder()
            .regionAttributes(new HashMap<>(Map.of("label", "BATI", "confidence", "0.9")))
            .shapeAttributes(
                DetectionResponseV2.ImageData.ShapeAttributes.builder()
                    .name("polygon")
                    .allPointsX(
                        List.of(
                            BigDecimal.valueOf(10),
                            BigDecimal.valueOf(900),
                            BigDecimal.valueOf(500)))
                    .allPointsY(
                        List.of(
                            BigDecimal.valueOf(10),
                            BigDecimal.valueOf(40),
                            BigDecimal.valueOf(800)))
                    .build())
            .build());
    Map<String, DetectionResponseV2.ImageData> images = new HashMap<>();
    images.put(
        "primer_tile",
        DetectionResponseV2.ImageData.builder()
            .filename("primer_tile.jpg")
            .fileAttributes(new HashMap<>())
            .regions(regions)
            .build());
    return DetectionResponseV2.builder().images(images).build();
  }

  private static void deleteQuietly(File file) {
    if (file == null) {
      return;
    }
    org.apache.commons.io.FileUtils.deleteQuietly(file);
    var parent = file.getParentFile();
    if (parent != null && parent.isDirectory()) {
      parent.delete();
    }
  }
}
