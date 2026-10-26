package app.bpartners.geojobs.service.lidar;

import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
public class LasFileCleaner {
  public void clean(File directory) {
    if (!directory.exists()) {
      return;
    }

    try {
      FileUtils.deleteDirectory(directory);
    } catch (IOException e) {
      log.warn("Cannot delete folder {}", directory.getAbsolutePath(), e);
    }
  }
}
