package app.bpartners.geojobs.utils.detection;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.service.JobAnnotationService;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.JobFinishedMailer;
import app.bpartners.geojobs.service.TaskToJobConverter;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.utils.ParcelCreator;
import app.bpartners.geojobs.utils.TileCreator;
import app.bpartners.geojobs.utils.tiling.ZoneTilingJobCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class DetectionIT extends FacadeIT {
  @Autowired protected ZoneTilingJobRepository ztjRepository;
  @Autowired protected ParcelRepository parcelRepository;
  @Autowired protected ParcelDetectionJobService pdjService;
  @Autowired protected ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  @Autowired protected ZoneDetectionJobService zdjService;

  @Autowired
  protected TaskToJobConverter<ParcelDetectionTask, ParcelDetectionJob> taskToJobConverter;

  @Autowired protected ZoneDetectionJobRepository zdjRepository;
  @Autowired protected ParcelDetectionJobRepository pdjRepository;
  @MockBean protected TileObjectDetector objectsDetectorMock;
  @MockBean protected JobAnnotationService jobAnnotationServiceMock;
  @MockBean protected JobFinishedMailer<ZoneDetectionJob> mailerMock;
  protected final ParcelCreator parcelCreator = new ParcelCreator();
  protected final TileCreator tileCreator = new TileCreator();
  protected final TileDetectionTaskCreator tileDetectionTaskCreator =
      new TileDetectionTaskCreator();
  protected final ParcelDetectionJobCreator parcelDetectionJobCreator =
      new ParcelDetectionJobCreator();
  protected final ParcelDetectionTaskCreator parcelDetectionTaskCreator =
      new ParcelDetectionTaskCreator();
  protected final ZoneDetectionJobCreator zoneDetectionJobCreator = new ZoneDetectionJobCreator();
  protected final ZoneTilingJobCreator zoneTilingJobCreator = new ZoneTilingJobCreator();

  protected ZoneDetectionJob dummyZoneDetectionJob(
      String detectionJobId,
      ZoneTilingJob tilingJob,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    return zoneDetectionJobCreator.create(
        detectionJobId,
        "dummyZoneName",
        "dummyEmailReceiver",
        progressionStatus,
        healthStatus,
        tilingJob);
  }

  protected ZoneDetectionJob pendingZoneDetectionJob(
      String detectionJobId, ZoneTilingJob tilingJob) {
    return dummyZoneDetectionJob(detectionJobId, tilingJob, PENDING, UNKNOWN);
  }

  protected ZoneDetectionJob processingZoneDetectionJob(
      String detectionJobId, ZoneTilingJob tilingJob) {
    return dummyZoneDetectionJob(detectionJobId, tilingJob, PROCESSING, UNKNOWN);
  }

  protected ZoneDetectionJob succeededZoneDetectionJob(
      String detectionJobId, ZoneTilingJob tilingJob) {
    return dummyZoneDetectionJob(detectionJobId, tilingJob, FINISHED, SUCCEEDED);
  }

  protected ZoneTilingJob finishedZoneTilingJob(String tilingJobId) {
    return zoneTilingJobCreator.create(
        tilingJobId, "dummyZoneName", "dummyEmailReceiver", FINISHED, SUCCEEDED);
  }
}
