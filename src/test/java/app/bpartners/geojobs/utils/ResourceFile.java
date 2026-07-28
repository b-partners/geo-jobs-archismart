package app.bpartners.geojobs.utils;

import java.io.File;

public class ResourceFile {
  public static File getResourceFile(String path) {
    var url = ResourceFile.class.getClassLoader().getResource(path);
    if (url == null) {
      throw new IllegalArgumentException("Resource not found: " + path);
    }
    return new File(url.getFile());
  }
}
