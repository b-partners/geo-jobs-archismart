package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionExcelFileAddressConverted;
import app.bpartners.geojobs.endpoint.event.model.DetectionExcelFileSaved;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.ExcelAddressConverter;
import app.bpartners.geojobs.service.event.DetectionExcelFileSavedService;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DetectionExcelFileSavedServiceTest {
  BucketComponent bucketComponentMock = mock();
  ExcelAddressConverter excelAddressConverterMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  DetectionExcelFileSavedService subject =
      new DetectionExcelFileSavedService(
          bucketComponentMock,
          excelAddressConverterMock,
          detectionRepositoryMock,
          eventProducerMock);

  @Test
  void produces_detection_excel_file_address_converted() {
    var excelFileMock = mock(File.class);
    var retrievedAddresses = List.of("dummyAddress1", "dummyAddress2");
    var excelFileKey = "excelFileKey";
    var detection = Detection.builder().excelFileKey(excelFileKey).build();
    when(bucketComponentMock.download(excelFileKey)).thenReturn(excelFileMock);
    when(excelAddressConverterMock.apply(excelFileMock, null)).thenReturn(retrievedAddresses);
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    assertDoesNotThrow(
        () -> subject.accept(DetectionExcelFileSaved.builder().detection(detection).build()));

    var eventListCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(eventListCaptor.capture());
    var detectionExcelFileAddressConverted =
        (DetectionExcelFileAddressConverted) eventListCaptor.getValue().getFirst();
    var expectedDetectionExcelFileAddressConverted =
        DetectionExcelFileAddressConverted.builder()
            .detection(detection.toBuilder().convertedAddresses(retrievedAddresses).build())
            .build();
    assertEquals(expectedDetectionExcelFileAddressConverted, detectionExcelFileAddressConverted);
  }
}
