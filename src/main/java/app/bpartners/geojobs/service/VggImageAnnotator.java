package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VggImageAnnotator {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @SneakyThrows
  public BufferedImage annotate(File vggFile, File imageFile) {
    BufferedImage image = ImageIO.read(imageFile);
    if (image == null) {
      throw new IOException("Not a valid image file: " + imageFile);
    }
    Graphics2D g2d = image.createGraphics();
    g2d.setStroke(new BasicStroke(4));
    g2d.setFont(new Font("Arial", Font.PLAIN, 36));

    JsonNode root = objectMapper.readTree(vggFile);
    if (!root.isArray()) {
      throw new IllegalArgumentException("VGG file must contain a list of elements: " + vggFile);
    }

    int addressCount = 0;
    for (JsonNode element : root) {
      Iterator<Map.Entry<String, JsonNode>> uuidEntries = element.fields();
      while (uuidEntries.hasNext()) {
        JsonNode vggData = uuidEntries.next().getValue();
        JsonNode properties = vggData.get("properties");
        JsonNode regions = vggData.get("regions");

        String firstAddress = null;
        if (properties != null
            && properties.has("addresses")
            && properties.get("addresses").isArray()
            && properties.get("addresses").size() > 0) {
          firstAddress = properties.get("addresses").get(0).asText();
        }

        int firstPolygonX = -1;
        int firstPolygonY = -1;
        if (regions != null && regions.isObject()) {
          Iterator<Map.Entry<String, JsonNode>> regionEntries = regions.fields();
          while (regionEntries.hasNext()) {
            JsonNode regionNode = regionEntries.next().getValue();
            JsonNode shape = regionNode.get("shape_attributes");
            JsonNode region = regionNode.get("region_attributes");
            if (shape == null || !"Polygon".equalsIgnoreCase(shape.get("name").asText())) {
              continue;
            }

            JsonNode xPointsNode = shape.get("all_points_x");
            JsonNode yPointsNode = shape.get("all_points_y");
            int numPoints = xPointsNode.size();
            int[] xPoints = new int[numPoints];
            int[] yPoints = new int[numPoints];
            for (int i = 0; i < numPoints; i++) {
              xPoints[i] = (int) Math.round(xPointsNode.get(i).asDouble());
              yPoints[i] = (int) Math.round(yPointsNode.get(i).asDouble());
            }

            String label =
                region != null && region.has("label") ? region.get("label").asText() : null;
            g2d.setColor(Color.decode(getColorFromDetectedType(parseDetectableType(label))));
            g2d.drawPolygon(new Polygon(xPoints, yPoints, numPoints));

            if (firstPolygonX == -1 && firstPolygonY == -1) {
              firstPolygonX = Arrays.stream(xPoints).min().orElse(0);
              firstPolygonY = Arrays.stream(yPoints).min().orElse(0) - 15;
            }
          }

          if (firstAddress != null
              && !firstAddress.isBlank()
              && firstPolygonX >= 0
              && firstPolygonY >= 0) {
            g2d.setColor(Color.BLUE);
            g2d.drawString(firstAddress, firstPolygonX, firstPolygonY + (addressCount * 40));
            addressCount++;
          }
        }
      }
    }

    g2d.dispose();
    return image;
  }

  public File annotate(File vggFile, File imageFile, File output) throws IOException {
    ImageIO.write(annotate(vggFile, imageFile), "png", output);
    log.info("annotated image written -> {}", output);
    return output;
  }

  private static DetectableType parseDetectableType(String label) {
    if (label == null) {
      return null;
    }
    try {
      return DetectableType.valueOf(label.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static String getColorFromDetectedType(DetectableType detectableType) {
    if (detectableType == null) {
      return "#8C8B89"; // grey
    }
    return switch (detectableType) {
      case ARBRE, ESPACE_VERT, ESPACE_VERT_PARKING -> "#4CAF50"; // green

      case MOISISSURE, MOISISSURE_CLAIR, MOISISSURE_COULEUR, MOISISSURE_NOIRCIE ->
          "#795548"; // Maroon

      case HUMIDITE, HUMIDITE_CLAIR, HUMIDITE_INTENSE -> "#2196F3"; // blue

      case USURE, USURE_IMPORTANTE, USURE_LEGER -> "#F44336"; // red

      case OBSTACLE, VELUX, CHEMINEE -> "#000000"; // black

      case TOITURE_REVETEMENT -> "#db531d"; // maroon

      // Other
      default -> "#8C8B89"; // grey
    };
  }
}
