package app.bpartners.geojobs.service.detection.inference;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.time.Duration.ofHours;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

@Slf4j
@EnabledIfEnvironmentVariable(named = "INFERENCE_API_KEY", matches = ".+")
class InferenceApiLocalIT {
  private static final String TILE_RESOURCE = "images/tile-1.jpg";

  InferenceApiClient client;
  ModelCardRegistry registry;

  @BeforeEach
  void setUp() {
    var url = envOrDefault("INFERENCE_API_URL", null);
    client = new InferenceApiClient(new RestTemplate(), url, System.getenv("INFERENCE_API_KEY"));
    registry = new ModelCardRegistry(client, ofHours(8));
  }

  @Test
  void list_model_cards() {
    var modelCards = client.getModelCards();

    modelCards.forEach(modelCard -> log.info("Model card: {}", modelCard));
    assertFalse(modelCards.isEmpty());
    modelCards.forEach(
        modelCard -> {
          assertNotNull(modelCard.modelCardId());
          assertNotNull(modelCard.problemType());
        });
  }

  @Test
  void registry_loads_runnable_model_cards_once() {
    var runnable = registry.getRunnableModelCards();

    log.info("Runnable model cards: {}", runnable.keySet());
    assertFalse(runnable.isEmpty());
    runnable.values().forEach(modelCard -> assertTrue(modelCard.isRunnable()));
    var first = runnable.keySet().iterator().next();
    assertEquals(runnable.get(first), registry.findById(first).orElseThrow());
  }

  @Test
  @SneakyThrows
  void infer_with_single_image_model_card() {
    var modelCard = pickSingleImageModelCard();
    log.info("Running inference with model card {}", modelCard);
    var width = modelCard.imageWidth() == null ? 1024 : modelCard.imageWidth();
    var height = modelCard.imageHeight() == null ? 1024 : modelCard.imageHeight();
    var tile =
        resize(ImageIO.read(new ClassPathResource(TILE_RESOURCE).getInputStream()), width, height);

    var response =
        client.infer(
            InferenceRequest.builder()
                .modelCardId(modelCard.modelCardId())
                .image1(toBase64(tile, "jpg"))
                .mask1(modelCard.maskRequired() ? toBase64(whiteMask(width, height), "png") : null)
                .filename("local-it-tile.jpg")
                .zoomLevel(Integer.parseInt(envOrDefault("INFERENCE_ZOOM_LEVEL", "21")))
                .spatialResolution(
                    Double.parseDouble(envOrDefault("INFERENCE_SPATIAL_RESOLUTION", "5")))
                .build());

    assertNotNull(response);
    assertFalse(response.getImages().isEmpty());
    response
        .getImages()
        .forEach(
            (filename, image) -> {
              var regions = image.getRegions() == null ? 0 : image.getRegions().size();
              log.info("Image {} -> {} regions", filename, regions);
              if (image.getRegions() != null) {
                image
                    .getRegions()
                    .values()
                    .forEach(
                        region ->
                            log.info(
                                "  label={} points={}",
                                region.getRegionAttributes(),
                                region.getShapeAttributes() == null
                                    ? 0
                                    : region.getShapeAttributes().getAllPointsX().size()));
              }
            });
  }

  private DeployedModelCard pickSingleImageModelCard() {
    var wanted = System.getenv("INFERENCE_MODEL_CARD_ID");
    if (wanted != null && !wanted.isBlank()) {
      return registry
          .findById(wanted)
          .orElseThrow(() -> new IllegalStateException("Model card not runnable: " + wanted));
    }
    return registry.getRunnableModelCards().values().stream()
        .filter(modelCard -> modelCard.imagesRequired() == 1)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No runnable single-image model card"));
  }

  private static BufferedImage resize(BufferedImage source, int width, int height) {
    if (source.getWidth() == width && source.getHeight() == height) {
      return source;
    }
    var resized = new BufferedImage(width, height, TYPE_INT_RGB);
    var graphics = resized.createGraphics();
    graphics.drawImage(source, 0, 0, width, height, null);
    graphics.dispose();
    return resized;
  }

  private static BufferedImage whiteMask(int width, int height) {
    var mask = new BufferedImage(width, height, TYPE_BYTE_GRAY);
    var graphics = mask.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, width, height);
    graphics.dispose();
    return mask;
  }

  @SneakyThrows
  private static String toBase64(BufferedImage image, String format) {
    var output = new ByteArrayOutputStream();
    ImageIO.write(image, format, output);
    return Base64.getEncoder().encodeToString(output.toByteArray());
  }

  private static String envOrDefault(String name, String defaultValue) {
    var value = System.getenv(name);
    return value == null || value.isBlank() ? defaultValue : value;
  }
}
