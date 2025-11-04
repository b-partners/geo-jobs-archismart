package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static java.awt.AlphaComposite.Src;
import static java.awt.Color.*;
import static java.awt.Font.PLAIN;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

import app.bpartners.geojobs.model.DetectedObjectTypeWithPolygon;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.BiFunction;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class DetectedImageDraw
    implements BiFunction<File, List<DetectedObjectTypeWithPolygon>, File> {

  @SneakyThrows
  @Override
  public File apply(
      File originalImage, List<DetectedObjectTypeWithPolygon> detectedObjectTypeWithPolygon) {
    if (detectedObjectTypeWithPolygon.isEmpty()) {
      return originalImage;
    }
    BufferedImage original = ImageIO.read(originalImage);
    if (original == null)
      throw new IOException("Not valid image file found for " + originalImage.getName());
    BufferedImage newImage =
        new BufferedImage(original.getWidth(), original.getHeight(), TYPE_INT_ARGB);
    Graphics2D g2d = newImage.createGraphics();
    g2d.drawImage(original, 0, 0, null);
    g2d.setStroke(new BasicStroke(4));
    g2d.setFont(new Font("Arial", PLAIN, 16));
    g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON); // For best rendering
    g2d.setComposite(Src); // For transparent color

    // Write polygons
    for (DetectedObjectTypeWithPolygon polygon : detectedObjectTypeWithPolygon) {
      if (TOITURE_REVETEMENT.equals(polygon.objectType())) {
        continue;
      }
      g2d.setColor(getColor(polygon.objectType()));
      List<app.bpartners.geojobs.endpoint.rest.model.Point> points = polygon.pointList();
      int[] xPoints =
          points.stream().mapToInt(point -> point.getCoordinates().getFirst().intValue()).toArray();
      int[] yPoints =
          points.stream().mapToInt(point -> point.getCoordinates().getLast().intValue()).toArray();
      if (points.size() >= 3) {
        g2d.drawPolygon(xPoints, yPoints, points.size());
      } else if (points.size() == 2) {
        g2d.drawLine(xPoints[0], yPoints[0], xPoints[1], yPoints[1]);
      } else if (points.size() == 1) {
        g2d.fillOval(xPoints[0] - 2, yPoints[0] - 2, 4, 4);
      }
    }

    // Write legends
    var detectableTypes =
        detectedObjectTypeWithPolygon.stream()
            .map(DetectedObjectTypeWithPolygon::objectType)
            .toList();
    int legendX = newImage.getWidth() - 200;
    int legendY = newImage.getHeight() - 20 * detectableTypes.size();
    int lineHeight = 20;
    for (int i = 0; i < detectableTypes.size(); i++) {
      Color color = getColor(detectableTypes.get(i));
      String label = detectableTypes.toString();
      int y = legendY + i * lineHeight;
      g2d.setColor(color);
      g2d.fillRect(legendX, y, 15, 15);
      g2d.drawString(label, legendX + 20, y + 12);
    }

    g2d.dispose();

    var outputFile = Files.createTempFile(originalImage.getName(), ".jpg").toFile();
    ImageIO.write(newImage, "jpg", outputFile);
    return outputFile;
  }

  // TODO : set specific color for each type
  private Color getColor(DetectableType detectableType) {
    return switch (detectableType) {
      case HUMIDITE, HUMIDITE_CLAIR, HUMIDITE_INTENSE -> BLUE;
      case MOISISSURE, MOISISSURE_CLAIR, MOISISSURE_COULEUR, MOISISSURE_NOIRCIE -> BLACK;
      case OBSTACLE, CHEMINEE, VELUX -> ORANGE;
      case USURE, USURE_IMPORTANTE, USURE_LEGER -> new Color(208, 25, 88);
      case ARBRE, ESPACE_VERT -> GREEN;
      case PANNEAU_PHOTOVOLTAIQUE -> CYAN;
      case RISQUE_FEU -> new Color(255, 0, 0);
      default -> new Color(0, 0, 0, 0);
    };
  }
}
