package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.ESPACE_VERT;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Disabled("TODO: local use only")
@Slf4j
class VggPolygonDrawer {

  @SneakyThrows
  @Test
  void draw_polygon_on_file() {
    File vggFile = new ClassPathResource("vgg/polygon-1.json").getFile();
    File imageFile = new ClassPathResource("vgg/image-1.jpg").getFile();

    var actual = drawPolygonsOnImage(vggFile, imageFile);

    assertNotNull(actual);
  }

  private File drawPolygonsOnImage(File vggFile, File imageFile) throws Exception {
    // Lire l'image
    BufferedImage image = ImageIO.read(imageFile);
    Graphics2D g2d = image.createGraphics();
    g2d.setStroke(new BasicStroke(4));
    g2d.setFont(new Font("Arial", Font.PLAIN, 36));

    // Lire le JSON
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(vggFile);

    if (!root.isArray()) {
      throw new IllegalArgumentException("Le fichier VGG doit contenir une liste d'éléments");
    }

    int addressCount = 0; // Compteur pour espacer les adresses

    for (JsonNode element : root) {
      // Chaque élément contient une clé UUID dynamique
      Iterator<Map.Entry<String, JsonNode>> uuidEntries = element.fields();
      while (uuidEntries.hasNext()) {
        Map.Entry<String, JsonNode> uuidEntry = uuidEntries.next();
        JsonNode vggData = uuidEntry.getValue();

        JsonNode properties = vggData.get("properties");
        JsonNode regions = vggData.get("regions");

        // Récupérer la première adresse si disponible
        String firstAddress = null;
        if (properties != null
            && properties.has("addresses")
            && properties.get("addresses").isArray()) {
          JsonNode addressesNode = properties.get("addresses");
          if (addressesNode.size() > 0) {
            firstAddress = addressesNode.get(0).asText();
          }
        }

        int firstPolygonX = -1;
        int firstPolygonY = -1;

        if (regions != null && regions.isObject()) {
          Iterator<Map.Entry<String, JsonNode>> regionEntries = regions.fields();
          while (regionEntries.hasNext()) {
            Map.Entry<String, JsonNode> regionEntry = regionEntries.next();
            JsonNode regionNode = regionEntry.getValue();
            JsonNode shape = regionNode.get("shape_attributes");
            JsonNode region = regionNode.get("region_attributes");

            if (shape != null && "Polygon".equalsIgnoreCase(shape.get("name").asText())) {
              JsonNode xPointsNode = shape.get("all_points_x");
              JsonNode yPointsNode = shape.get("all_points_y");

              int numPoints = xPointsNode.size();
              int[] xPoints = new int[numPoints];
              int[] yPoints = new int[numPoints];

              for (int i = 0; i < numPoints; i++) {
                xPoints[i] = (int) Math.round(xPointsNode.get(i).asDouble());
                yPoints[i] = (int) Math.round(yPointsNode.get(i).asDouble());
              }

              String label = region.has("label") ? region.get("label").asText() : "unknown";
              var detectableType = DetectableType.valueOf(label.toUpperCase());
              var color = getColorFromDetectedType(detectableType);

              Polygon polygon = new Polygon(xPoints, yPoints, numPoints);
              g2d.setColor(color != null ? Color.decode(color) : Color.RED);
              if (detectableType != ESPACE_VERT) {
                g2d.drawPolygon(polygon);
              }

              // Retenir la première position pour l’adresse
              if (firstPolygonX == -1 && firstPolygonY == -1) {
                firstPolygonX = Arrays.stream(xPoints).min().orElse(0);
                firstPolygonY = Arrays.stream(yPoints).min().orElse(0) - 15;
              }
            }
          }

          // Afficher l’adresse une seule fois par VGG, avec espacement
          if (firstAddress != null
              && !firstAddress.isBlank()
              && firstPolygonX >= 0
              && firstPolygonY >= 0) {
            g2d.setColor(Color.BLUE);

            // Décalage vertical selon le nombre d’adresses déjà affichées
            int spacing = 40; // espace entre deux textes
            int textY = firstPolygonY + (addressCount * spacing);

            g2d.drawString(firstAddress, firstPolygonX, textY);
            addressCount++;
          }
        }
      }
    }

    g2d.dispose();

    // Sauvegarde du résultat
    File output = new File("image_annotated-2.png");
    ImageIO.write(image, "png", output);
    return output;
  }

  private static String getColorFromDetectedType(DetectableType detectableType) {
    return switch (detectableType) {

      // 🌿 Espaces verts & arbres
      case ARBRE, ESPACE_VERT, ESPACE_VERT_PARKING -> "#4CAF50"; // vert

      // 🍄 Moisissures
      case MOISISSURE, MOISISSURE_CLAIR, MOISISSURE_COULEUR, MOISISSURE_NOIRCIE ->
          "#9C27B0"; // violet

      // 💧 Humidité
      case HUMIDITE, HUMIDITE_CLAIR, HUMIDITE_INTENSE -> "#2196F3"; // bleu

      // ⚠️ Usure
      case USURE, USURE_IMPORTANTE, USURE_LEGER -> "#F44336"; // rouge

      // 🏠 Toiture
      case TOITURE_REVETEMENT,
              CHEMINEE,
              VELUX,
              BATI_TUILES,
              BATI_BETON,
              BATI_ARDOISE,
              BATI_AUTRES ->
          "#795548"; // marron

      // ⬜ Tout le reste
      default -> "#9E9E9E"; // gris
    };
  }
}
