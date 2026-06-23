package app.bpartners.geojobs.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.BiFunction;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Superimposes a detection mask on top of its original tile image.
 *
 * <p>The mask is expected to follow the convention produced by {@link
 * app.bpartners.geojobs.service.detection.DetectionMaskCreator} (used through {@link
 * DetectionMaskFromTileRetriever}): a black background with detected regions painted as non-black
 * pixels. Any pixel of the mask brighter than {@link #maskLuminanceThreshold} is considered part of
 * the detection and is tinted with {@link #overlayColor} at {@link #overlayOpacity}, leaving the
 * rest of the original image untouched.
 *
 * <p>The first argument is the original image, the second is the corresponding mask. The mask is
 * resampled to the original size when their dimensions differ.
 */
@Slf4j
public class ImageMaskOverlay implements BiFunction<File, File, BufferedImage> {
  // Vivid magenta contrasts strongly with aerial imagery (greens/greys/browns/reds).
  private static final Color DEFAULT_OVERLAY_COLOR = new Color(255, 0, 255);
  private static final float DEFAULT_OVERLAY_OPACITY = 0.6f;
  // Mask background is black (0,0,0) and detected regions are non-black (e.g. (1,1,1)), so any
  // strictly-positive luminance marks a detected pixel.
  private static final int DEFAULT_MASK_LUMINANCE_THRESHOLD = 0;

  private final Color overlayColor;
  private final float overlayOpacity;
  private final int maskLuminanceThreshold;

  public ImageMaskOverlay() {
    this(DEFAULT_OVERLAY_COLOR, DEFAULT_OVERLAY_OPACITY, DEFAULT_MASK_LUMINANCE_THRESHOLD);
  }

  public ImageMaskOverlay(Color overlayColor, float overlayOpacity, int maskLuminanceThreshold) {
    this.overlayColor = overlayColor;
    this.overlayOpacity = overlayOpacity;
    this.maskLuminanceThreshold = maskLuminanceThreshold;
  }

  @Override
  @SneakyThrows
  public BufferedImage apply(File originalImageFile, File maskFile) {
    var original = ImageIO.read(originalImageFile);
    var mask = ImageIO.read(maskFile);
    if (original == null) {
      throw new IllegalArgumentException("Cannot read original image: " + originalImageFile);
    }
    if (mask == null) {
      throw new IllegalArgumentException("Cannot read mask: " + maskFile);
    }
    return apply(original, mask);
  }

  public BufferedImage apply(BufferedImage original, BufferedImage mask) {
    var width = original.getWidth();
    var height = original.getHeight();
    var overlay = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = overlay.createGraphics();
    g2d.drawImage(original, 0, 0, width, height, null);
    g2d.dispose();

    var overlayRed = overlayColor.getRed();
    var overlayGreen = overlayColor.getGreen();
    var overlayBlue = overlayColor.getBlue();
    var maskScaleX = (double) mask.getWidth() / width;
    var maskScaleY = (double) mask.getHeight() / height;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        var maskX = (int) (x * maskScaleX);
        var maskY = (int) (y * maskScaleY);
        if (!isDetectedPixel(mask.getRGB(maskX, maskY))) {
          continue;
        }
        var rgb = overlay.getRGB(x, y);
        var red = (rgb >> 16) & 0xFF;
        var green = (rgb >> 8) & 0xFF;
        var blue = rgb & 0xFF;
        var blendedRed = blend(red, overlayRed);
        var blendedGreen = blend(green, overlayGreen);
        var blendedBlue = blend(blue, overlayBlue);
        overlay.setRGB(x, y, (blendedRed << 16) | (blendedGreen << 8) | blendedBlue);
      }
    }
    return overlay;
  }

  private int blend(int base, int over) {
    return Math.round(base * (1 - overlayOpacity) + over * overlayOpacity);
  }

  private boolean isDetectedPixel(int rgb) {
    var red = (rgb >> 16) & 0xFF;
    var green = (rgb >> 8) & 0xFF;
    var blue = rgb & 0xFF;
    return Math.max(red, Math.max(green, blue)) > maskLuminanceThreshold;
  }
}
