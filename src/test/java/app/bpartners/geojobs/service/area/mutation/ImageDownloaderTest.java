package app.bpartners.geojobs.service.area.mutation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageDownloaderTest {
  private final ImageDownloader subject = new ImageDownloader();

  @TempDir Path tempDir;

  @SneakyThrows
  @Test
  void download_writes_the_source_bytes_to_a_local_file() {
    var sourceBytes = new byte[] {1, 2, 3, 4, 5};
    var sourceFile = tempDir.resolve("source.jpg");
    Files.write(sourceFile, sourceBytes);

    var downloaded = subject.download(sourceFile.toUri().toString());

    assertTrue(downloaded.exists());
    assertArrayEquals(sourceBytes, Files.readAllBytes(downloaded.toPath()));
  }
}
