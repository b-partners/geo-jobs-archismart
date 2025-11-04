package app.bpartners.geojobs.file.zip;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.time.Instant.now;

import app.bpartners.geojobs.model.exception.ApiException;
import java.io.*;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class FileZipper implements Function<List<File>, ZipFile> {

  public static final String ZIP_FILE_SUFFIX = ".zip";

  @Override
  @SneakyThrows
  public ZipFile apply(List<File> files) {
    File zipFile = File.createTempFile("zip-file-" + now(), ZIP_FILE_SUFFIX);
    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos)) {
      byte[] buffer = new byte[1024];
      files.forEach(
          file -> {
            if (!file.isDirectory()) {
              try {
                zos.putNextEntry(new ZipEntry(file.getName()));
                try (FileInputStream fis = new FileInputStream(file)) {
                  int len;
                  while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                  }
                }
              } catch (IOException e) {
                throw new ApiException(SERVER_EXCEPTION, e);
              }
            }
          });
      zos.finish();
      zos.flush();
      return new ZipFile(zipFile);
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}
