package app.bpartners.geojobs.unit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Disabled("TODO: local use only")
class Base64ImageConverter {

  @Test
  void base64_image_to_bytes() {
    File imageBase64File;
    try {
      imageBase64File = new ClassPathResource("images/imageBase64").getFile();
      var base64 = Files.readString(imageBase64File.toPath());
      if (base64.contains(",")) {
        base64 = base64.substring(base64.indexOf(",") + 1);
      }

      byte[] imageBytes = Base64.getDecoder().decode(base64);
      Files.write(Path.of(new File("imageBytes.jpg").getPath()), imageBytes);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
