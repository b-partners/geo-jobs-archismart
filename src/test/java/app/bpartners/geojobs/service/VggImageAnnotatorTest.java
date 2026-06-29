package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

class VggImageAnnotatorTest {
  private final VggImageAnnotator subject = new VggImageAnnotator();

  private File vggFile() throws IOException {
    return new ClassPathResource("vgg/polygon-1.json").getFile();
  }

  private File imageFile() throws IOException {
    return new ClassPathResource("vgg/image-1.jpg").getFile();
  }

  @Test
  void annotate_returns_image_with_source_dimensions() throws Exception {
    var source = ImageIO.read(imageFile());

    var annotated = subject.annotate(vggFile(), imageFile());

    assertNotNull(annotated);
    assertEquals(source.getWidth(), annotated.getWidth());
    assertEquals(source.getHeight(), annotated.getHeight());
  }

  @Test
  void annotate_writes_a_readable_png(@TempDir Path tmp) throws Exception {
    var output = tmp.resolve("annotated.png").toFile();

    var returned = subject.annotate(vggFile(), imageFile(), output);

    assertEquals(output, returned);
    assertTrue(output.exists() && output.length() > 0, "output png should not be empty");
    assertNotNull(ImageIO.read(output), "output should be a valid image");
  }

  @Test
  void non_array_vgg_is_rejected(@TempDir Path tmp) throws Exception {
    var notAnArray = tmp.resolve("vgg.json");
    Files.writeString(notAnArray, "{\"foo\":\"bar\"}");

    assertThrows(
        IllegalArgumentException.class, () -> subject.annotate(notAnArray.toFile(), imageFile()));
  }

  @Test
  void invalid_image_is_rejected(@TempDir Path tmp) throws Exception {
    var notAnImage = tmp.resolve("not-an-image.jpg");
    Files.writeString(notAnImage, "this is not an image");

    assertThrows(IOException.class, () -> subject.annotate(vggFile(), notAnImage.toFile()));
  }
}
