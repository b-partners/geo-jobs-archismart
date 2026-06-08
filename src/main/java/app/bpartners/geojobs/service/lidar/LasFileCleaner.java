package app.bpartners.geojobs.service.lidar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LasFileCleaner {
  public void clean(File directory) {
    if (!directory.exists()) {
      return;
    }

    try {
      Files.deleteIfExists(directory.toPath());
    } catch (IOException e) {
      log.warn("Cannot delete folder {}", directory.getAbsolutePath());
    }
  }
}
