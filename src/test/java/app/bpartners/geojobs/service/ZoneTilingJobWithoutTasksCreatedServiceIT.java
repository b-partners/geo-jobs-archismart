package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobWithoutTasksCreated;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.event.ZoneTilingJobWithoutTasksCreatedService;
import app.bpartners.geojobs.service.tiling.TilingJobDuplicatedMailer;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(isolation = Isolation.SERIALIZABLE)
class ZoneTilingJobWithoutTasksCreatedServiceIT extends FacadeIT {
  private final String jobId;
  private final String tilingTask1Id;
  private final String tilingTask2Id;
  private final String duplicatedJobId;
  @Autowired ZoneTilingJobWithoutTasksCreatedService subject;
  @Autowired ZoneTilingJobRepository zoneTilingJobRepository;
  @Autowired TilingTaskRepository taskRepository;
  @Autowired ZoneDetectionJobRepository detectionJobRepository;
  @MockBean EventProducer eventProducer;
  @MockBean TilingJobDuplicatedMailer tilingJobDuplicatedMailerMock;

  public ZoneTilingJobWithoutTasksCreatedServiceIT() {
    this.jobId = randomUUID().toString();
    this.tilingTask1Id = randomUUID().toString();
    this.tilingTask2Id = randomUUID().toString();
    this.duplicatedJobId = randomUUID().toString();
  }

  @BeforeEach
  void setUp() {
    zoneTilingJobRepository.save(
        ZoneTilingJob.builder()
            .id(jobId)
            .emailReceiver("dummy@email.com")
            .zoneName("dummyZoneName")
            .build());
    ParcelTilingTask taskWithoutParcel =
        ParcelTilingTask.builder()
            .id(tilingTask1Id)
            .jobId(jobId)
            .parcels(List.of())
            .statusHistory(
                List.of(
                    TaskStatus.builder()
                        .id(randomUUID().toString())
                        .taskId(tilingTask2Id)
                        .progression(Status.ProgressionStatus.PENDING)
                        .health(Status.HealthStatus.UNKNOWN)
                        .creationDatetime(now())
                        .build()))
            .build();
    ParcelTilingTask taskWithParcel =
        ParcelTilingTask.builder()
            .id(tilingTask2Id)
            .jobId(jobId)
            .statusHistory(
                List.of(
                    TaskStatus.builder()
                        .id(randomUUID().toString())
                        .taskId(tilingTask2Id)
                        .progression(Status.ProgressionStatus.PENDING)
                        .health(Status.HealthStatus.UNKNOWN)
                        .creationDatetime(now())
                        .build()))
            .parcels(
                List.of(
                    Parcel.builder()
                        .id(randomUUID().toString())
                        .parcelContent(
                            ParcelContent.builder()
                                .id(randomUUID().toString())
                                .tiles(List.of(new Tile()))
                                .build())
                        .build()))
            .build();
    taskRepository.saveAll(List.of(taskWithoutParcel, taskWithParcel));
  }

  @AfterEach
  void tearDown() {
    taskRepository.deleteAllById(List.of(tilingTask1Id, tilingTask2Id));
    zoneTilingJobRepository.deleteById(jobId);
  }

  @Test
  void accept_ok() {
    var ztj = zoneTilingJobRepository.getById(jobId);
    var emptyInitialJob = zoneTilingJobRepository.findById(duplicatedJobId);
    var emptyInitialTasks = taskRepository.findAllByJobId(duplicatedJobId);
    var expectedTasks = taskRepository.findAllByJobId(jobId);

    subject.accept(
        ZoneTilingJobWithoutTasksCreated.builder()
            .originalJob(ztj)
            .duplicatedJobId(duplicatedJobId)
            .build());

    var actualDuplicatedJob = zoneTilingJobRepository.findById(duplicatedJobId).orElseThrow();
    var actualDuplicatedTasks = taskRepository.findAllByJobId(duplicatedJobId);
    var associatedDetectionJobs = detectionJobRepository.findAllByZoneTilingJob_Id(duplicatedJobId);
    assertTrue(emptyInitialJob.isEmpty());
    assertTrue(emptyInitialTasks.isEmpty());
    assertEquals(expectedTasks.size(), actualDuplicatedTasks.size());
    assertEquals(ztj.duplicate(duplicatedJobId), actualDuplicatedJob);
    assertEquals(2, associatedDetectionJobs.size());
    assertTrue(
        associatedDetectionJobs.stream().anyMatch(job -> job.getDetectionType().equals(HUMAN)));
    assertTrue(
        associatedDetectionJobs.stream().anyMatch(job -> job.getDetectionType().equals(MACHINE)));
    verify(tilingJobDuplicatedMailerMock, times(1)).accept(any());
  }
}
