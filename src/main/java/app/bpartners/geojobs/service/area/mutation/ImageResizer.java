package app.bpartners.geojobs.service.area.mutation;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static java.util.UUID.randomUUID;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

/** Resizes an image file to a fixed square size, e.g. to match a model's expected input. */
@Component
public class ImageResizer {

  /** Smooth interpolation, suited for photographic old/new area pictures. */
  public File resizePhoto(File imageFile, int size) {
    return resize(imageFile, size, Image.SCALE_SMOOTH);
  }

  /**
   * Nearest-neighbor interpolation: keeps mask pixels pure black/white instead of blurring edges
   * into intermediate gray values, which would break the mutation model's binary mask handling.
   */
  public File resizeMask(File imageFile, int size) {
    return resize(imageFile, size, Image.SCALE_REPLICATE);
  }

  @SneakyThrows
  private File resize(File imageFile, int size, int scaleMode) {
    var original = ImageIO.read(imageFile);
    var scaled = original.getScaledInstance(size, size, scaleMode);
    var resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    var g2d = resized.createGraphics();
    g2d.drawImage(scaled, 0, 0, null);
    g2d.dispose();

    var output = File.createTempFile("resized_" + randomUUID(), ".png", createTempDirectory());
    ImageIO.write(resized, "png", output);
    return output;
  }
}
