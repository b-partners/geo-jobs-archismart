package app.bpartners.geojobs.service.area.mutation;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static java.util.UUID.randomUUID;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class ImageDownloader {
  @SneakyThrows
  public File download(String imageUrl) {
    var destination =
        File.createTempFile("area_picture_" + randomUUID(), ".jpg", createTempDirectory());
    try (var in = new URI(imageUrl).toURL().openStream();
        var out = new FileOutputStream(destination)) {
      in.transferTo(out);
    }
    return destination;
  }
}
