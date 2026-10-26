package app.bpartners.geojobs.file.zip;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.ImageValidator;
import app.bpartners.geojobs.model.exception.ApiException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FileUnzipper implements BiFunction<ZipFile, String, Path> {
  private final FileWriter fileWriter;
  private final ImageValidator imageValidator;

  @Override
  public Path apply(ZipFile zipFile, String mainDir) {
    try {
      Path extractDirectoryPath = createIsolatedTmpDirectory(mainDir);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (!entry.isDirectory()) {
          try (InputStream is = zipFile.getInputStream(entry)) {
            String entryParentPath = getFolderPath(entry);
            String entryFilename = getFilename(entry);
            String extensionlessEntryFilename = stripExtension(entryFilename);
            Path destinationPath = extractDirectoryPath.resolve(entryParentPath);
            byte[] bytes = is.readAllBytes();
            Optional.of(
                    fileWriter.write(bytes, destinationPath.toFile(), extensionlessEntryFilename))
                .map(this::fromFile)
                .ifPresent(imageValidator);
          }
        }
      }

      return extractDirectoryPath;
    } catch (IOException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  /**
   * Returns tmpdir/<random>/<mainDir>: the directory keeps the mainDir name (used as bucket key by
   * the tiling) but each call gets its own one. A shared tmpdir/<mainDir> mixed the tiles of
   * concurrent and previous jobs on the same layer.
   */
  private static Path createIsolatedTmpDirectory(String mainDir) throws IOException {
    // owner-only (rwx------) random directory; Path.of joins mainDir even when it starts with "/"
    Path extractDirectoryPath = Path.of(FileWriter.createTempDirectory().getPath(), mainDir);
    Files.createDirectories(extractDirectoryPath);
    return extractDirectoryPath;
  }

  private static String getFolderPath(ZipEntry zipEntry) {
    String entryPath = zipEntry.getName();
    Path normalizedPath = Paths.get(entryPath).normalize();

    if (normalizedPath.startsWith("..")) {
      throw new IllegalArgumentException("Path traversal attempt detected");
    }

    Path parentPath = normalizedPath.getParent();
    return (parentPath != null) ? parentPath.toString() : "";
  }

  private static String getFilename(ZipEntry zipEntry) {
    String entryPath = zipEntry.getName();
    return Paths.get(entryPath).getFileName().toString();
  }

  public static String stripExtension(String filename) {
    return filename.substring(0, filename.lastIndexOf("."));
  }

  private BufferedImage fromFile(File file) {
    try {
      return ImageIO.read(file);
    } catch (IOException e) {
      return null;
    }
  }
}
