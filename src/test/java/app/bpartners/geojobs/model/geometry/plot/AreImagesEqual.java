package app.bpartners.geojobs.model.geometry.plot;

import java.awt.image.BufferedImage;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AreImagesEqual implements BiFunction<BufferedImage, BufferedImage, Boolean> {

  private final double threshold;

  @Override
  public Boolean apply(BufferedImage imgA, BufferedImage imgB) {
    if (imgA.getWidth() != imgB.getWidth() || imgA.getHeight() != imgB.getHeight()) {
      return false;
    }

    int differingPixel = 0;
    int width = imgA.getWidth();
    int height = imgA.getHeight();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
          differingPixel++;
        }
      }
    }

    return differingPixel / ((double) width * height) < threshold;
  }
}
