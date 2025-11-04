package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ZoneDetectionJobServiceMockedIT extends FacadeIT {
  @Autowired ZoneDetectionJobService service;
  @MockBean ZoneDetectionJobRepository repository;
  @MockBean ParcelDetectionTaskRepository taskRepository;
  @MockBean EventProducer eventProducer;

  private void setMockData(String jobId) {
    when(taskRepository.findAllByJobId(jobId)).thenReturn(List.of(parcelDetectionTask(jobId)));
  }

  private ParcelDetectionTask parcelDetectionTask(String jobId) {
    var taskStatusHistory = new ArrayList<TaskStatus>();
    taskStatusHistory.add(
        TaskStatus.builder()
            .id(randomUUID().toString())
            .progression(PENDING)
            .jobType(DETECTION)
            .health(UNKNOWN)
            .build());
    return ParcelDetectionTask.builder()
        .id(randomUUID().toString())
        .jobId(jobId)
        .submissionInstant(now())
        .parcels(
            List.of(
                Parcel.builder()
                    .id(randomUUID().toString())
                    .parcelContent(
                        ParcelContent.builder()
                            .id(randomUUID().toString())
                            .tiles(
                                List.of(
                                    Tile.builder()
                                        .id(randomUUID().toString())
                                        .bucketPath(randomUUID().toString())
                                        .build()))
                            .build())
                    .build()))
        .statusHistory(taskStatusHistory)
        .build();
  }

  public ZoneDetectionJob aZDJ(String jobId) {
    var statusHistory = new ArrayList<JobStatus>();
    statusHistory.add(
        JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(jobId)
            .jobType(DETECTION)
            .progression(PENDING)
            .health(UNKNOWN)
            .build());
    return ZoneDetectionJob.builder().id(jobId).statusHistory(statusHistory).build();
  }

  @Test
  void process_zdj() {
    var jobId = randomUUID().toString();
    var job = aZDJ(jobId);
    setMockData(jobId);
    when(repository.findById(any())).thenReturn(Optional.of(job));
    when(repository.save(any())).thenReturn(job);

    ZoneDetectionJob actual = service.fireTasks(jobId);

    assertNotNull(actual.getId());
    verify(eventProducer, times(3)).accept(any());
  }
}
