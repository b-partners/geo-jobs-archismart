package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.ExtensionGuesser;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import app.bpartners.geojobs.service.DetectionAddressConversionTaskToCsvConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class DetectionAddressConversionTaskToCsvConverterTest {
  FileWriter fileWriter =
      new FileWriter(new ObjectMapper().findAndRegisterModules(), new ExtensionGuesser());
  DetectionAddressConversionTaskToCsvConverter subject =
      new DetectionAddressConversionTaskToCsvConverter(fileWriter);

  @SneakyThrows
  @Test
  void convert_detection_address_task_to_csv() {
    var taskMock1 = mock(DetectionAddressConversionTask.class);
    var taskMock2 = mock(DetectionAddressConversionTask.class);
    var taskMock3 = mock(DetectionAddressConversionTask.class);
    when(taskMock1.getAddress()).thenReturn("address1");
    when(taskMock2.getAddress()).thenReturn("address2");
    when(taskMock3.getAddress()).thenReturn(null);
    var mockTasks = List.of(taskMock1, taskMock2, taskMock3);

    var actual = subject.apply(mockTasks, "Excel soumis lors de la détection X");

    var actualContent = Files.readString(actual.toPath());
    assertTrue(actualContent.contains("Adresse\n"));
    assertTrue(actualContent.contains("address1\n"));
    assertTrue(actualContent.contains("address2\n"));
  }
}
