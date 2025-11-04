package app.bpartners.geojobs.service.event;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.COMPLETED;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.gen.annotator.endpoint.rest.model.Job;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.utils.LogCaptor;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnnotationRetrieverTest {
  private final HumanDetectionJobRepository humanDetectionJobRepositoryMock = mock();
  private final AnnotationService annotationServiceMock = mock();
  private final AnnotationRetrievingJobService annotationRetrievingJobServiceMock = mock();
  private final LogCaptor logCaptor = new LogCaptor();
  private final AnnotationRetriever subject =
      new AnnotationRetriever(
          humanDetectionJobRepositoryMock,
          annotationServiceMock,
          annotationRetrievingJobServiceMock);

  @BeforeEach
  void setUp() {
    logCaptor.configure();
  }

  @Test
  void accept_on_empty_detection_job() {
    when(humanDetectionJobRepositoryMock.findAllByZoneDetectionJobId(any())).thenReturn(List.of());
    var humanZdjId = randomUUID().toString();

    subject.accept(humanZdjId);
    List<ILoggingEvent> logEvents = logCaptor.getLogEvents();

    assertTrue(
        logEvents.stream()
            .anyMatch(
                event ->
                    event
                        .getFormattedMessage()
                        .contains("DEBUG: retrieving annotation, humanDetectionJobs")));
    assertTrue(
        logEvents.stream()
            .anyMatch(
                event ->
                    event
                        .getFormattedMessage()
                        .contains("DEBUG: aborting retrieving, humanDetectionJobs empty")));
  }

  @Test
  void accept_on_empty_annotation_job() {
    when(humanDetectionJobRepositoryMock.findAllByZoneDetectionJobId(any()))
        .thenReturn(List.of(humanDetectionJob()));
    when(annotationServiceMock.getAnnotationJobById(any())).thenReturn(new Job().status(COMPLETED));
    var humanZdjId = randomUUID().toString();

    subject.accept(humanZdjId);
    List<ILoggingEvent> logEvents = logCaptor.getLogEvents();

    assertTrue(
        logEvents.stream()
            .anyMatch(
                event ->
                    event
                        .getFormattedMessage()
                        .contains("DEBUG: retrieving annotation, humanDetectionJobs")));
    verify(annotationServiceMock, times(1))
        .retrieveTasksFromAnnotationJob(any(), any(), any(), any(), any(), any());
  }

  private HumanDetectionJob humanDetectionJob() {
    return HumanDetectionJob.builder()
        .machineDetectedTiles(List.of(new MachineDetectedTile()))
        .build();
  }
}
