package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.file.ImageValidator;
import app.bpartners.geojobs.file.WhiteImageDetector;
import app.bpartners.geojobs.model.exception.BadRequestException;
import java.io.IOException;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("TODO: flaky test")
class ImageValidatorTest {
  ImageValidator subject = new ImageValidator(new WhiteImageDetector());

  @Test
  void throws_exception_when_white_images_detected() throws IOException {
    var whiteImage1 =
        ImageIO.read(
            Objects.requireNonNull(
                this.getClass().getResourceAsStream("/images/white-image-1.png")));
    var whiteImage2 =
        ImageIO.read(
            Objects.requireNonNull(
                this.getClass().getResourceAsStream("/images/white-image-2.jpg")));

    var actualException1 =
        assertThrows(BadRequestException.class, () -> subject.accept(whiteImage1));
    var actualException2 =
        assertThrows(BadRequestException.class, () -> subject.accept(whiteImage2));
    assertEquals(actualException1.getMessage(), actualException2.getMessage());
    assertEquals("Invalid white image detected", actualException1.getMessage());
  }

  @Test
  void do_nothing_when_not_white_images_detected() throws IOException {
    var validImage =
        ImageIO.read(
            Objects.requireNonNull(this.getClass().getResourceAsStream("/images/tile-1.jpg")));

    assertDoesNotThrow(() -> subject.accept(validImage));
  }
}
