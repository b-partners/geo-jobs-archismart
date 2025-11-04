package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobCreated;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.utils.detection.ZoneDetectionJobCreator;
import app.bpartners.geojobs.utils.tiling.ZoneTilingJobCreator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ZoneDetectionJobCreatedServiceIT extends FacadeIT {
  @Autowired ZoneDetectionJobCreatedService subject;
  @Autowired ZoneDetectionJobService zoneDetectionJobService;
  @MockBean ParcelDetectionTaskRepository taskRepository;
  @MockBean EventProducer eventProducer;
  @Autowired ZoneDetectionJobRepository repository;
  ZoneTilingJobCreator zoneTilingJobCreator = new ZoneTilingJobCreator();
  ZoneDetectionJobCreator zoneDetectionJobCreator = new ZoneDetectionJobCreator();

  @BeforeEach
  void setUp() {
    String taskId = randomUUID().toString();
    when(taskRepository.findAllByJobId(any()))
        .thenReturn(
            List.of(
                ParcelDetectionTask.builder()
                    .id(taskId)
                    .jobId(randomUUID().toString())
                    .submissionInstant(now())
                    .statusHistory(
                        List.of(
                            TaskStatus.builder()
                                .id(randomUUID().toString())
                                .progression(PENDING)
                                .health(UNKNOWN)
                                .jobType(DETECTION)
                                .taskId(taskId)
                                .creationDatetime(now())
                                .build()))
                    .build()));
  }

  @Test
  void accept() {
    String tilingJobId = randomUUID().toString();
    String detectionJobId = randomUUID().toString();
    ZoneTilingJob tilingJob =
        zoneTilingJobCreator.create(tilingJobId, "mock", "mock@gmail.com", FINISHED, SUCCEEDED);
    ZoneDetectionJob detectionJob =
        zoneDetectionJobCreator.create(
            detectionJobId, "mock", "mock@gmail.com", PENDING, UNKNOWN, tilingJob);
    ZoneDetectionJob savedZDJ = repository.save(detectionJob);

    subject.accept(ZoneDetectionJobCreated.builder().zoneDetectionJob(savedZDJ).build());

    ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(3)).accept(listCaptor.capture());
    assertEquals(
        ParcelDetectionTaskCreated.class,
        (listCaptor.getAllValues().getFirst().getFirst()).getClass());
    assertEquals(
        ZDJStatusRecomputingSubmitted.class,
        (listCaptor.getAllValues().get(1).getFirst()).getClass());
    assertEquals(
        AutoTaskStatisticRecomputingSubmitted.class,
        (listCaptor.getAllValues().get(2).getFirst()).getClass());
    ZoneDetectionJob actualJob = zoneDetectionJobService.findById(savedZDJ.getId());
    assertEquals(MACHINE, actualJob.getDetectionType());
    assertEquals(PENDING, actualJob.getStatus().getProgression());
    assertEquals(UNKNOWN, actualJob.getStatus().getHealth());
  }
}
