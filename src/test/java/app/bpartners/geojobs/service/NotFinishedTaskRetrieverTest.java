package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.RETRYING;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import java.util.List;
import org.junit.jupiter.api.Test;

public class NotFinishedTaskRetrieverTest {
  NotFinishedTaskRetriever<ParcelTilingTask> tilingSubject = new NotFinishedTaskRetriever<>();

  @Test
  void not_finished_tiling_task_ok() {
    ParcelTilingTask parcelTilingTask =
        ParcelTilingTask.builder().id("tilingTaskId").statusHistory(List.of()).build();

    ParcelTilingTask actual = tilingSubject.apply(parcelTilingTask);

    assertEquals(
        Status.builder()
            .id(randomUUID().toString())
            .progression(PENDING)
            .health(RETRYING)
            .creationDatetime(null)
            .build(),
        actual.getStatus().toBuilder().creationDatetime(null).build());
  }
}
