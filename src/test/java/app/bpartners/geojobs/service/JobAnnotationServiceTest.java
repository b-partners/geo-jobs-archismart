package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import app.bpartners.geojobs.endpoint.rest.model.AnnotationJobProcessing;
import app.bpartners.geojobs.job.service.AnnotationDetectionJobProcessing;
import app.bpartners.geojobs.job.service.JobAnnotationService;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JobAnnotationServiceTest {
  private static final String TILING_JOB = "tilingJob";
  private static final String DETECTION_JOB_ID = "detectionJobId";
  private static final double MIN_CONFIDENCE = 0.8;
  ZoneTilingJobRepository tilingJobRepositoryMock = mock();
  ZoneDetectionJobRepository zoneDetectionJobRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  JobAnnotationService subject =
      new JobAnnotationService(
          zoneDetectionJobRepositoryMock,
          tilingJobRepositoryMock,
          new AnnotationDetectionJobProcessing(eventProducerMock));

  @Test
  void process_annotation_job_ko() {
    when(tilingJobRepositoryMock.findById(TILING_JOB))
        .thenReturn(Optional.of(ZoneTilingJob.builder().build()));
    assertThrows(
        NotImplementedException.class,
        () -> subject.processAnnotationJob(TILING_JOB, MIN_CONFIDENCE));
  }

  @Test
  void process_annotation_job_ok() {
    when(zoneDetectionJobRepositoryMock.findById(DETECTION_JOB_ID))
        .thenReturn(Optional.of(ZoneDetectionJob.builder().id(DETECTION_JOB_ID).build()));

    AnnotationJobProcessing actual = subject.processAnnotationJob(DETECTION_JOB_ID, MIN_CONFIDENCE);

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(eventCaptor.capture());
    List<AnnotationDeliveryJobRequested> events =
        (List<AnnotationDeliveryJobRequested>) eventCaptor.getValue();
    AnnotationDeliveryJobRequested event = events.getFirst();
    assertEquals(
        event.getAnnotationJobWithObjectsIdTruePositive(),
        actual.getAnnotationWithObjectTruePositive());
    assertEquals(
        event.getAnnotationJobWithoutObjectsId(), actual.getAnnotationWithoutObjectJobId());
    assertEquals(
        event.getAnnotationJobWithObjectsIdFalsePositive(),
        actual.getAnnotationWithObjectFalsePositive());
    assertEquals(event.getJobId(), actual.getJobId());
    assertEquals(DETECTION_JOB_ID, actual.getJobId());
    assertEquals(MIN_CONFIDENCE, event.getMinimumConfidenceForDelivery());
  }
}
