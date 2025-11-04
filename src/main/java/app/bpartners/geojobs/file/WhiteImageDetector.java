package app.bpartners.geojobs.file;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class WhiteImageDetector implements Function<BufferedImage, Boolean> {

  @Override
  public Boolean apply(BufferedImage img) {
    int width = img.getWidth();
    int height = img.getHeight();
    int totalPixels = width * height;
    int[] channel = img.getRGB(0, 0, width, height, null, 0, width);
    int[] histogram = new int[256];

    Arrays.stream(channel).map(pixel -> (pixel >> 8) & 0xFF).forEach(value -> histogram[value]++);

    int highestPeak = Arrays.stream(histogram).max().orElse(0);
    int highestPeakIndex = Arrays.stream(histogram).boxed().toList().indexOf(highestPeak);

    double highestPeakRatio = (double) highestPeak / totalPixels;

    return highestPeakIndex == 255 && highestPeakRatio > 0.8;
  }
}
