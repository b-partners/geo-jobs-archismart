package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PASSAGE_PIETON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskCreated;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.service.TileDetectionTaskConsumer;
import app.bpartners.geojobs.service.detection.TileDetectionTaskStatusService;
import java.util.List;
import org.junit.jupiter.api.Test;

class TileDetectionTaskCreatedServiceTest {
  TileDetectionTaskStatusService tileDetectionTaskStatusServiceMock = mock();
  TileDetectionTaskConsumer tileDetectionTaskConsumerMock = mock();
  TileDetectionTaskRepository tileDetectionTaskRepositoryMock = mock();
  TileDetectionTaskCreatedService subject =
      new TileDetectionTaskCreatedService(
          tileDetectionTaskConsumerMock,
          tileDetectionTaskStatusServiceMock,
          tileDetectionTaskRepositoryMock);

  @Test
  void consumes_task_and_succeed() {
    doNothing().when(tileDetectionTaskConsumerMock).accept(any());
    when(tileDetectionTaskStatusServiceMock.process(any()))
        .thenReturn(
            TileDetectionTask.builder()
                .statusHistory(
                    List.of(TaskStatus.builder().progression(PROCESSING).health(UNKNOWN).build()))
                .build());
    when(tileDetectionTaskStatusServiceMock.succeed(any()))
        .thenReturn(
            TileDetectionTask.builder()
                .statusHistory(
                    List.of(TaskStatus.builder().progression(FINISHED).health(SUCCEEDED).build()))
                .build());

    assertDoesNotThrow(
        () ->
            subject.accept(
                new TileDetectionTaskCreated(
                    "zdjId",
                    TileDetectionTask.builder()
                        .statusHistory(
                            List.of(
                                TaskStatus.builder()
                                    .progression(FINISHED)
                                    .health(SUCCEEDED)
                                    .build()))
                        .build(),
                    List.of(
                        DetectableObjectConfiguration.builder().objectType(PASSAGE_PIETON).build()),
                    null,
                    null)));
    verify(tileDetectionTaskStatusServiceMock, only()).succeed(any());
    verify(tileDetectionTaskRepositoryMock, only()).save(any());
  }

  @Test
  void accept_ko_fails_fast_without_retry() {
    doThrow(ApiException.class).when(tileDetectionTaskConsumerMock).accept(any());
    TileDetectionTaskCreated expectedTileDetectionTaskCreated =
        new TileDetectionTaskCreated(
            "zdjId",
            TileDetectionTask.builder().build(),
            List.of(DetectableObjectConfiguration.builder().objectType(PASSAGE_PIETON).build()),
            null,
            null);

    // fail-fast: the exception is swallowed (no rethrow) so the message is acked, not retried
    assertDoesNotThrow(() -> subject.accept(expectedTileDetectionTaskCreated));
    verify(tileDetectionTaskStatusServiceMock, never()).succeed(any());
    verify(tileDetectionTaskStatusServiceMock).fail(any());
    verify(tileDetectionTaskRepositoryMock).save(any());
  }
}
