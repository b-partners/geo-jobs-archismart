package app.bpartners.geojobs.service.lidar;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.junit.jupiter.api.Test;

class LasFileCleanerTest {
  private final LasFileCleaner cleaner = new LasFileCleaner();

  @Test
  void should_delete_existing_directory() throws Exception {
    var dir = createTempDirectory();
    assertTrue(dir.exists());

    cleaner.clean(dir);

    assertFalse(dir.exists());
  }

  @Test
  void should_not_throw_when_directory_does_not_exist() {
    var dir = new File("non-existing-dir-");
    assertDoesNotThrow(() -> cleaner.clean(dir));
  }
}
