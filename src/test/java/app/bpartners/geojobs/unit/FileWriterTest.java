package app.bpartners.geojobs.unit;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.file.ExtensionGuesser;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.model.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FileWriterTest {

  private ExtensionGuesser extensionGuesser;
  private FileWriter fileWriter;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    extensionGuesser = mock(ExtensionGuesser.class);
    objectMapper = new ObjectMapper();
    fileWriter = new FileWriter(objectMapper, extensionGuesser);
  }

  @Test
  void testApplyThrowsApiExceptionOnIOException() {
    var bytes = new byte[] {1, 2, 3};

    when(extensionGuesser.apply(bytes)).thenReturn("txt");
    Mockito.mockStatic(File.class)
        .when(() -> File.createTempFile(anyString(), anyString(), eq(null)))
        .thenThrow(new IOException());

    assertThrows(
        app.bpartners.geojobs.model.exception.ApiException.class,
        () -> fileWriter.apply(bytes, null));
  }

  @Test
  void testWriteThrowsIllegalArgumentExceptionForInvalidDirectory() {
    var bytes = new byte[] {1, 2, 3};
    var invalidDirectory = new File("../");
    var filename = "testfile";

    assertThrows(
        IllegalArgumentException.class, () -> fileWriter.write(bytes, invalidDirectory, filename));
  }

  @Test
  void testWriteThrowsDirectoryNull() {
    var bytes = new byte[] {1, 2, 3};
    var filename = "testfile";

    assertThrows(ApiException.class, () -> fileWriter.write(bytes, null, filename));
  }

  @Test
  void testWriteThrowsApiExceptionOnIOException() throws IOException {
    var bytes = new byte[] {1, 2, 3};
    var directory = new File("validDirectory");
    var filename = "testfile";

    when(extensionGuesser.apply(bytes)).thenReturn("txt");
    Files.createDirectories(directory.toPath());
    Mockito.mockStatic(Files.class)
        .when(() -> Files.write(any(), eq(bytes)))
        .thenThrow(new IOException());

    assertThrows(ApiException.class, () -> fileWriter.write(bytes, directory, filename));
  }
}
