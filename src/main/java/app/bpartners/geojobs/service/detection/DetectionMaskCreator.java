package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static java.awt.Color.BLACK;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.model.geometry.IntXY;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DetectionMaskCreator implements Function<List<List<BigDecimal>>, File> {
  private static final int DEFAULT_IMAGE_SIZE = 1024;

  @SneakyThrows
  private File drawImage(List<IntXY> pixels) {
    BufferedImage image =
        new BufferedImage(DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = image.createGraphics();

    g2d.setColor(BLACK);
    g2d.fillRect(0, 0, DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE);

    if (!pixels.isEmpty()) {
      int[] xPoints = pixels.stream().mapToInt(IntXY::x).toArray();
      int[] yPoints = pixels.stream().mapToInt(IntXY::y).toArray();

      if (xPoints.length > 2) {
        var color = new Color(1, 1, 1);
        g2d.setColor(color);
        g2d.fillPolygon(xPoints, yPoints, xPoints.length);

        g2d.setColor(color);
        g2d.drawPolygon(xPoints, yPoints, xPoints.length);
      }
    }
    g2d.dispose();
    File output = File.createTempFile("mask_" + randomUUID(), ".png", createTempDirectory());
    ImageIO.write(image, "png", output);
    log.info("Mask created at {}", output.getAbsolutePath());
    return output;
  }

  @Override
  public File apply(List<List<BigDecimal>> pixelPolygon) {
    var pixels =
        pixelPolygon.stream()
            .map(list -> new IntXY(list.getFirst().intValue(), list.getLast().intValue()))
            .toList();
    return drawImage(pixels);
  }

  @SneakyThrows
  public File createTempMask() {
    BufferedImage whiteImage =
        new BufferedImage(DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
    var color = new Color(1, 1, 1);

    for (int y = 0; y < DEFAULT_IMAGE_SIZE; y++) {
      for (int x = 0; x < DEFAULT_IMAGE_SIZE; x++) {
        whiteImage.setRGB(x, y, color.getRGB());
      }
    }

    File output = File.createTempFile(randomUUID().toString(), ".png");
    ImageIO.write(whiteImage, "png", output);
    return output;
  }
}
