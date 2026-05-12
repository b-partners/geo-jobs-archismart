package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.TileDuplicationRemover;
import java.util.*;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MachineDetectionCreation
    implements BiFunction<
        Detection, ZoneTilingJob, app.bpartners.geojobs.endpoint.rest.model.Detection> {
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final ZoneDetectionJobValidator detectionJobValidator;
  private final DetectionMachineDetectionStatisticsComputer
      detectionMachineDetectionStatisticsComputer;
  private final TileDuplicationRemover tileDuplicationRemover;

  @Override
  public app.bpartners.geojobs.endpoint.rest.model.Detection apply(
      Detection detection, ZoneTilingJob zoneTilingJob) {
    var zoneDetectionJob = zoneDetectionJobService.getByTilingJobId(zoneTilingJob.getId(), MACHINE);

    detectionJobValidator.accept(zoneDetectionJob.getId());

    var savedZoneDetectionJob =
        zoneDetectionJobService.processZDJ(
            zoneDetectionJob.getId(), detection.getDetectableObjectConfigurations());

    return detectionMachineDetectionStatisticsComputer.apply(
        detection, savedZoneDetectionJob.getId());
  }

  public void processMachineDetection(
      Detection detection, ZoneDetectionJob zoneDetectionJob, List<ParcelTilingTask> tilingTasks) {
    var tilesWithDuplicatedCoordinates =
        tilingTasks.stream().map(ParcelTilingTask::getTiles).flatMap(List::stream).toList();
    var tiles = tileDuplicationRemover.apply(tilesWithDuplicatedCoordinates);
    var tileDetectionTasks =
        tiles.stream()
            .map(
                tile -> {
                  TileDetectionTask tileDetectionTask =
                      new TileDetectionTask(null, null, null, null, tile, new ArrayList<>());
                  tileDetectionTask.setZoneDetectionJobId(zoneDetectionJob.getId());
                  tileDetectionTask.setDetectableObjectConfigurations(
                      detection.getDetectableObjectConfigurations());
                  return tileDetectionTask;
                })
            .toList();

    zoneDetectionJobService.consumeTasks(tileDetectionTasks);

    ArrayList<JobStatus> statusHistory = new ArrayList<>();
    statusHistory.add(
        JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(zoneDetectionJob.getId())
            .progression(FINISHED)
            .health(SUCCEEDED)
            .jobType(DETECTION)
            .creationDatetime(now())
            .build());
    zoneDetectionJobService.save(zoneDetectionJob.toBuilder().statusHistory(statusHistory).build());
  }
}
