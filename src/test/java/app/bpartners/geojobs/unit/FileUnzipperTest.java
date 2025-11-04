package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.file.zip.FileZipper.ZIP_FILE_SUFFIX;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.ImageValidator;
import app.bpartners.geojobs.file.zip.FileUnzipper;
import app.bpartners.geojobs.model.exception.ApiException;
import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FileUnzipperTest extends FacadeIT {
  @Mock FileUnzipper fileUnzipper;
  @Mock FileWriter fileWriter;
  @Mock ImageValidator imageValidatorMock;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    doNothing().when(imageValidatorMock).accept(any());
    fileUnzipper = new FileUnzipper(fileWriter, imageValidatorMock);
  }

  ZipFile zipFileIllegalArgumentException() throws IOException {
    var zipFile = File.createTempFile("maliciousZipFile", ZIP_FILE_SUFFIX);
    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos)) {
      zos.putNextEntry(new ZipEntry(".."));
      zos.finish();
      zos.flush();
      return new ZipFile(zipFile);
    }
  }

  void applyFileUnzipperIllegalArgumentException() throws IOException {
    fileUnzipper.apply(zipFileIllegalArgumentException(), "/mainDir");
  }

  @Test
  void getFolderPathIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> applyFileUnzipperIllegalArgumentException());
  }

  @Test
  void testThrowsApiExceptionOnIOException() {
    var zipFile = mock(ZipFile.class);
    var entries = mock(Enumeration.class);

    when(zipFile.entries()).thenReturn(entries);
    when(entries.hasMoreElements()).thenReturn(true);
    when(entries.nextElement())
        .thenThrow(
            new ApiException(
                ApiException.ExceptionType.SERVER_EXCEPTION,
                new IOException("Simulated IO error")));

    assertThrows(ApiException.class, () -> fileUnzipper.apply(zipFile, "mainDir"));
  }
}
