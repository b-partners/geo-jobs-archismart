package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class ImageMaskOverlayTest {
  // Sub-folder (under the files downloaded by DetectionFileObjectTest) to walk through.
  static final Path INPUT_DIRECTORY =
      Path.of("target", "downloaded-file-objects", "Haut-de-france", "20", "532433");
  static final Path OUTPUT_DIRECTORY = Path.of("target", "mask-overlays");

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ImageMaskOverlay imageMaskOverlay = new ImageMaskOverlay();

  @SneakyThrows
  @Test
  void overlay_masks_on_original_images() {
    assumeTrue(
        Files.isDirectory(INPUT_DIRECTORY),
        "no downloaded tiles in "
            + INPUT_DIRECTORY
            + " (run DetectionFileObjectTest#download_file_objects locally first)");

    List<Path> originalImages;
    try (Stream<Path> walk = Files.walk(INPUT_DIRECTORY)) {
      originalImages = walk.filter(ImageMaskOverlayTest::isOriginalImage).toList();
    }

    var overlaidCount = 0;
    for (var originalImage : originalImages) {
      // The downloaded _mask.png files are empty (all black): they carry no detected region. The
      // real geometry lives in the VGG-formatted _response_v2_*.json, so we rebuild the mask from
      // it.
      var detectionResult = resolveDetectionResult(originalImage);
      if (detectionResult == null) {
        log.info("Skipping {} : no matching _response_v2_*.json found", originalImage);
        continue;
      }

      var original = ImageIO.read(originalImage.toFile());
      var mask = buildMask(detectionResult, original.getWidth(), original.getHeight());
      var overlay = imageMaskOverlay.apply(original, mask);
      assertNotNull(overlay);

      var maskOutput = resolveOutput(originalImage, "_mask.png");
      var overlayOutput = resolveOutput(originalImage, "_overlay.png");
      Files.createDirectories(overlayOutput.getParent());
      ImageIO.write(mask, "png", maskOutput.toFile());
      ImageIO.write(overlay, "png", overlayOutput.toFile());
      log.info("Overlay of {} written to {}", originalImage, overlayOutput);
      overlaidCount++;
    }

    log.info("Generated {} overlay image(s) under {}", overlaidCount, OUTPUT_DIRECTORY);
  }

  // Builds a black mask with all detected polygons filled in white, following the convention
  // expected by ImageMaskOverlay (black background, non-black detected regions).
  @SneakyThrows
  private BufferedImage buildMask(Path detectionResult, int width, int height) {
    var mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    var g2d = mask.createGraphics();
    g2d.setColor(BLACK);
    g2d.fillRect(0, 0, width, height);
    g2d.setColor(WHITE);

    var root = objectMapper.readTree(detectionResult.toFile());
    var imageNode = root.elements().next(); // top-level key is the dynamic image name
    var regions = imageNode.get("regions");
    if (regions != null) {
      for (var region : regions) {
        var shape = region.get("shape_attributes");
        var xPoints = toIntArray(shape.get("all_points_x"));
        var yPoints = toIntArray(shape.get("all_points_y"));
        if (xPoints.length > 2 && !isDegenerate(xPoints, yPoints)) {
          g2d.fillPolygon(xPoints, yPoints, xPoints.length);
        }
      }
    }
    g2d.dispose();
    return mask;
  }

  private static int[] toIntArray(JsonNode arrayNode) {
    var values = new ArrayList<Integer>();
    arrayNode.forEach(node -> values.add(node.asInt()));
    return values.stream().mapToInt(Integer::intValue).toArray();
  }

  private static boolean isDegenerate(int[] xPoints, int[] yPoints) {
    for (int i = 0; i < xPoints.length; i++) {
      if (xPoints[i] != 0 || yPoints[i] != 0) {
        return false;
      }
    }
    return true;
  }

  // Original tile images are named "{y}.jpg" (no suffix), e.g. 358353.jpg.
  private static boolean isOriginalImage(Path path) {
    if (!Files.isRegularFile(path)) {
      return false;
    }
    var name = path.getFileName().toString();
    return name.endsWith(".jpg") && !name.contains("_");
  }

  // For "358353.jpg" finds "358353_response_v2_*.json" in the same directory.
  private Path resolveDetectionResult(Path originalImage) {
    var name = originalImage.getFileName().toString();
    var baseName = name.substring(0, name.lastIndexOf('.'));
    var prefix = baseName + "_response_v2_";
    try (Stream<Path> siblings = Files.list(originalImage.getParent())) {
      return siblings
          .filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().startsWith(prefix))
          .filter(p -> p.getFileName().toString().endsWith(".json"))
          .findFirst()
          .orElse(null);
    } catch (Exception e) {
      return null;
    }
  }

  private Path resolveOutput(Path originalImage, String suffix) {
    var relative = createTempDirectory().toPath().relativize(originalImage);
    var name = originalImage.getFileName().toString();
    var outputName = name.substring(0, name.lastIndexOf('.')) + suffix;
    var relativeParent = relative.getParent();
    var targetParent =
        relativeParent == null ? OUTPUT_DIRECTORY : OUTPUT_DIRECTORY.resolve(relativeParent);

    var resolvedPath = targetParent.resolve(outputName);

    log.info("Resolved path: {}", resolvedPath);

    return resolvedPath;
  }
}
