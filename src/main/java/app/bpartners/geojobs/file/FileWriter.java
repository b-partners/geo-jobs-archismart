package app.bpartners.geojobs.file;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.model.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FileWriter implements BiFunction<byte[], File, File> {
  private static final String TEMP_FOLDER_PERMISSION = "rwx------";
  private final ObjectMapper objectMapper;
  private final ExtensionGuesser extensionGuesser;

  public byte[] writeAsByte(Object object) {
    try {
      return objectMapper.writeValueAsBytes(object);
    } catch (JsonProcessingException e) {
      throw new ApiException(SERVER_EXCEPTION, "error during object conversion to bytes");
    }
  }

  @Override
  public File apply(byte[] bytes, @Nullable File directory) {
    try {
      String name = randomUUID().toString();
      String suffix = "." + extensionGuesser.apply(bytes);
      File tempFile = File.createTempFile(name, suffix, directory);
      return Files.write(tempFile.toPath(), bytes).toFile();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public File write(byte[] bytes, @Nullable File directory, String filename) {
    if (directory != null && directory.getName().contains("..")) {
      throw new IllegalArgumentException("name must not contain .. but receceived: pathValue");
    }
    try {
      String suffix = extensionGuesser.apply(bytes);
      File newFile = new File(directory, filename + suffix);
      Path newFilePath = newFile.toPath();
      Path parent = newFilePath.getParent();
      Files.createDirectories(parent);
      Path path = Files.write(newFilePath, bytes, CREATE, TRUNCATE_EXISTING);
      return path.toFile();
    } catch (IOException | NullPointerException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  @SneakyThrows
  public File base64ToFile(String base64, String filename) {
    byte[] decodedBytes = Base64.getDecoder().decode(base64);
    String suffix = extensionGuesser.apply(decodedBytes);
    File tmpFile = File.createTempFile(filename, suffix);
    return Files.write(tmpFile.toPath(), decodedBytes).toFile();
  }

  @SneakyThrows
  public static File createTempDirectory() {
    Path tempDir;

    if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
      FileAttribute<Set<PosixFilePermission>> attrs =
          PosixFilePermissions.asFileAttribute(
              PosixFilePermissions.fromString(TEMP_FOLDER_PERMISSION));
      tempDir = Files.createTempDirectory(randomUUID().toString(), attrs);
    } else {
      tempDir = Files.createTempDirectory(randomUUID().toString());
    }

    var dirFile = tempDir.toFile();
    dirFile.deleteOnExit();
    return dirFile;
  }

  public static File createTempFile(String prefix, String suffix) {
    try {
      File tempDir = createTempDirectory();

      Path tempFile = Files.createTempFile(tempDir.toPath(), prefix, suffix);

      File file = tempFile.toFile();
      file.deleteOnExit();

      return file;

    } catch (IOException e) {
      throw new IllegalStateException("Failed to create temp file", e);
    }
  }
}
