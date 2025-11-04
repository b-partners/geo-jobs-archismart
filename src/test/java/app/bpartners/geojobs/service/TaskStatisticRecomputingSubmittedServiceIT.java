package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.model.status.TaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.repository.TaskStatisticRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.event.TaskStatisticRecomputingSubmittedService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TaskStatisticRecomputingSubmittedServiceIT extends FacadeIT {
  private static final String JOB_ID = "taskStatisticJobId";
  @Autowired ZoneTilingJobRepository tilingJobRepository;
  @Autowired TilingTaskRepository tilingTaskRepository;
  @Autowired TaskStatisticRepository taskStatisticRepository;
  @Autowired TaskStatisticRecomputingSubmittedService subject;

  @BeforeEach
  void setUp() {
    tilingJobRepository.save(aZTJ(JOB_ID, PROCESSING, UNKNOWN));
    tilingTaskRepository.saveAll(
        List.of(
            aTilingTask(JOB_ID, "someTaskId1", PENDING, UNKNOWN),
            aTilingTask(JOB_ID, "someTaskId2", PENDING, UNKNOWN),
            aTilingTask(JOB_ID, "someTaskId3", PROCESSING, UNKNOWN),
            aTilingTask(JOB_ID, "someTaskId4", FINISHED, SUCCEEDED)));
  }

  @Test
  void accept_tiling_task_ok() {
    var before = taskStatisticRepository.findTopByJobIdOrderByUpdatedAtDesc(JOB_ID);

    assertDoesNotThrow(
        () -> subject.accept(TaskStatisticRecomputingSubmitted.builder().jobId(JOB_ID).build()));

    var actual = taskStatisticRepository.findTopByJobIdOrderByUpdatedAtDesc(JOB_ID);
    assertNull(before);
    assertEquals(
        TaskStatistic.builder()
            .id(actual.getId())
            .jobId(JOB_ID)
            .taskStatusStatistics(actual.getTaskStatusStatistics())
            .tilesCount(4)
            .jobType(actual.getJobType())
            .updatedAt(actual.getUpdatedAt())
            .build(),
        actual);
  }

  private static ZoneTilingJob aZTJ(
      String jobId, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return ZoneTilingJob.builder()
        .id(jobId)
        .zoneName("dummy")
        .emailReceiver("dummy")
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .jobId(jobId)
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .build()))
        .build();
  }

  private static ParcelTilingTask aTilingTask(
      String jobId,
      String taskId,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    return ParcelTilingTask.builder()
        .id(taskId)
        .jobId(jobId)
        .parcels(
            List.of(
                someParcel(
                    randomUUID().toString(), randomUUID().toString(), randomUUID().toString())))
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .taskId(taskId)
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .creationDatetime(now())
                    .build()))
        .build();
  }

  private static Parcel someParcel(String parcelId, String parcelContentId, String tileId) {
    return Parcel.builder()
        .id(parcelId)
        .parcelContent(
            ParcelContent.builder()
                .id(parcelContentId)
                .tiles(List.of(Tile.builder().id(tileId).bucketPath("dummyBucketPath").build()))
                .build())
        .build();
  }
}
