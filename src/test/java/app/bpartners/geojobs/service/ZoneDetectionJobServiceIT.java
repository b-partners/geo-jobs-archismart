package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.ParcelRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.utils.tiling.ZoneTilingJobCreator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(isolation = Isolation.SERIALIZABLE)
class ZoneDetectionJobServiceIT extends FacadeIT {
  @Autowired ZoneDetectionJobService service;
  @Autowired ParcelDetectionTaskRepository taskRepository;
  @Autowired ParcelRepository parcelRepository;
  ZoneTilingJobCreator zoneTilingJobCreator = new ZoneTilingJobCreator();

  @Test
  void save_and_read_zdj_with_tasks() {
    List<Parcel> parcels =
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
                                    .bucketPath("dummyTileBucketPath")
                                    .creationDatetime(now())
                                    .coordinates(new TileCoordinates())
                                    .build()))
                        .build())
                .build());
    parcelRepository.saveAll(parcels);
    List<ParcelTilingTask> parcelTilingTasks =
        List.of(new ParcelTilingTask().toBuilder().parcels(parcels).build());

    String jobId = randomUUID().toString();
    ZoneTilingJob zoneTilingJob = getZoneTilingJob();
    ZoneDetectionJob jobToSave =
        ZoneDetectionJob.builder()
            .id(jobId)
            .zoneName("dummyZoneName")
            .submissionInstant(now())
            .statusHistory(
                List.of(
                    JobStatus.builder()
                        .id(randomUUID().toString())
                        .jobId(jobId)
                        .jobType(DETECTION)
                        .progression(PENDING)
                        .health(UNKNOWN)
                        .build()))
            .detectionType(ZoneDetectionJob.DetectionType.MACHINE)
            .emailReceiver("ryan@hei.school")
            .zoneTilingJob(zoneTilingJob)
            .build();

    ZoneDetectionJob actual = service.saveWithTasks(parcelTilingTasks, jobToSave);

    List<ParcelDetectionTask> savedTasks = taskRepository.findAllByJobId(actual.getId());
    assertNotNull(actual);
    assertEquals(zoneTilingJob, actual.getZoneTilingJob());
    assertEquals(1, savedTasks.size());
    assertEquals(parcels, savedTasks.getFirst().getParcels());
    assertNotNull(savedTasks.getFirst().getParcel().getParcelContent().getFirstTile());
  }

  private ZoneTilingJob getZoneTilingJob() {
    return zoneTilingJobCreator.create(
        randomUUID().toString(), "dummy", "dummy", FINISHED, SUCCEEDED);
  }
}
