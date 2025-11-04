package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION_ADDRESS_CONVERSION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionExcelFileAddressConverted;
import app.bpartners.geojobs.endpoint.event.model.zone.DetectionAddressConversionJobCreated;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionJob;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.DetectionAddressConsumer;
import app.bpartners.geojobs.service.DetectionAddressConversionJobMapper;
import app.bpartners.geojobs.service.DetectionAddressConversionJobService;
import app.bpartners.geojobs.service.DetectionAddressConversionTaskMapper;
import app.bpartners.geojobs.service.event.DetectionExcelFileAddressConvertedService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DetectionExcelFileAddressConvertedServiceTest {
  DetectionAddressConversionJobMapper detectionAddressConversionJobMapper =
      new DetectionAddressConversionJobMapper();
  DetectionAddressConversionJobService detectionAddressConversionJobServiceMock = mock();
  EventProducer eventProducerMock = mock();
  DetectionAddressConversionTaskMapper detectionAddressConversionTaskMapper =
      new DetectionAddressConversionTaskMapper();

  DetectionExcelFileAddressConvertedService subject =
      new DetectionExcelFileAddressConvertedService(
          new DetectionAddressConsumer(
              detectionAddressConversionJobMapper,
              detectionAddressConversionJobServiceMock,
              eventProducerMock,
              detectionAddressConversionTaskMapper));

  @Test
  void create_detection_address_conversion_job_and_tasks() {
    when(detectionAddressConversionJobServiceMock.create(any(), any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    var detectionId = randomUUID().toString();
    var zoneName = "dummyZoneName";
    var emailReceiver = "dummyEmailReceiver";
    var convertedAddresses = List.of("dummyAddress1", "dummyAddress2");
    var detection =
        Detection.builder()
            .id(detectionId)
            .zoneName(zoneName)
            .emailReceiver(emailReceiver)
            .convertedAddresses(convertedAddresses)
            .build();

    assertDoesNotThrow(
        () ->
            subject.accept(
                DetectionExcelFileAddressConverted.builder().detection(detection).build()));

    var taskListCaptor = ArgumentCaptor.forClass(List.class);
    var eventListCaptor = ArgumentCaptor.forClass(List.class);
    verify(detectionAddressConversionJobServiceMock, only())
        .create(any(DetectionAddressConversionJob.class), taskListCaptor.capture());
    verify(eventProducerMock, only()).accept(eventListCaptor.capture());
    var addressConversionTasks = (List<DetectionAddressConversionTask>) taskListCaptor.getValue();
    var detectionAddressConversionJobCreated =
        (DetectionAddressConversionJobCreated) eventListCaptor.getValue().getFirst();
    var expectedDetectionAddressConversionJobCreated =
        exepectedDetectionAddressConversionJobCreated(
            detectionAddressConversionJobCreated, detectionId, emailReceiver, zoneName);
    var actualConversionJobId = detectionAddressConversionJobCreated.getJob().getId();
    assertEquals(2, addressConversionTasks.size());
    assertEquals(
        expectedDetectionAddressConversionJobCreated, detectionAddressConversionJobCreated);
    assertTrue(
        addressConversionTasks.stream()
            .allMatch(
                task ->
                    task.getJobId().equals(actualConversionJobId)
                        && task.isPending()
                        && task.getFeature() == null // Not computed here yet
                        && task.getJobType().equals(DETECTION_ADDRESS_CONVERSION)));
    assertTrue(convertedAddresses.contains(addressConversionTasks.getFirst().getAddress()));
    assertTrue(convertedAddresses.contains(addressConversionTasks.getLast().getAddress()));
  }

  private DetectionAddressConversionJobCreated exepectedDetectionAddressConversionJobCreated(
      DetectionAddressConversionJobCreated detectionAddressConversionJobCreated,
      String detectionId,
      String emailReceiver,
      String zoneName) {
    return DetectionAddressConversionJobCreated.builder()
        .job(
            someDetectionAddressConversionJob(
                detectionAddressConversionJobCreated, detectionId, emailReceiver, zoneName))
        .build();
  }

  private DetectionAddressConversionJob someDetectionAddressConversionJob(
      DetectionAddressConversionJobCreated detectionAddressConversionJobCreated,
      String detectionId,
      String emailReceiver,
      String zoneName) {
    var actualJob = detectionAddressConversionJobCreated.getJob();
    var actualJobId = actualJob.getId();
    var detectionAddressConversionJob =
        DetectionAddressConversionJob.builder()
            .id(actualJobId)
            .detectionId(detectionId)
            .emailReceiver(emailReceiver)
            .zoneName(zoneName)
            .submissionInstant(actualJob.getSubmissionInstant())
            .build();
    detectionAddressConversionJob.hasNewStatus(
        JobStatus.builder()
            .id(actualJob.getStatus().getId())
            .jobId(actualJobId)
            .creationDatetime(now())
            .jobType(DETECTION_ADDRESS_CONVERSION)
            .progression(PENDING)
            .health(UNKNOWN)
            .build());
    return detectionAddressConversionJob;
  }
}
