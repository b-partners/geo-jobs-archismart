package app.bpartners.geojobs.service.area.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageResizerTest {
  private static final int TARGET_SIZE = 32;

  private final ImageResizer subject = new ImageResizer();

  @TempDir Path tempDir;

  @SneakyThrows
  @Test
  void resizePhoto_outputs_a_square_image_at_the_target_size() {
    var sourceFile = writeSquareImage("photo.png", 1024, Color.RED);

    var resized = subject.resizePhoto(sourceFile, TARGET_SIZE);

    var resizedImage = ImageIO.read(resized);
    assertEquals(TARGET_SIZE, resizedImage.getWidth());
    assertEquals(TARGET_SIZE, resizedImage.getHeight());
  }

  @SneakyThrows
  @Test
  void resizeMask_outputs_a_square_image_at_the_target_size() {
    var sourceFile = writeSquareImage("mask.png", 1024, Color.WHITE);

    var resized = subject.resizeMask(sourceFile, TARGET_SIZE);

    var resizedImage = ImageIO.read(resized);
    assertEquals(TARGET_SIZE, resizedImage.getWidth());
    assertEquals(TARGET_SIZE, resizedImage.getHeight());
  }

  @SneakyThrows
  private File writeSquareImage(String name, int size, Color color) {
    var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    var g2d = image.createGraphics();
    g2d.setColor(color);
    g2d.fillRect(0, 0, size, size);
    g2d.dispose();

    var file = tempDir.resolve(name).toFile();
    ImageIO.write(image, "png", file);
    return file;
  }
}
