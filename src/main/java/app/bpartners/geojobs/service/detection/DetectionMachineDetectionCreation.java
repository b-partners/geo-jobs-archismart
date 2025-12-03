package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionMachineDetectionCreation
    implements BiFunction<
        Detection, ZoneTilingJob, app.bpartners.geojobs.endpoint.rest.model.Detection> {
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final ZoneDetectionJobValidator detectionJobValidator;
  private final DetectionMachineDetectionStatisticsComputer
      detectionMachineDetectionStatisticsComputer;
  private final GeometryConverter geometryConverter;

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
    var providedGeoJsonZone = detection.getProvidedGeoJsonZone();
    var providedLonLatJtsMultiPolygon =
        providedGeoJsonZone.stream()
            .map(
                feature -> {
                  switch (feature.getGeometry().getActualInstance()) {
                    case Point point -> {
                      if (detection.hasToitureModelName()) {
                        return geometryConverter.retrieveNearestRoofMultiPolygon(point);
                      }
                      log.warn("Skip processing detection on Point : {}", point);
                      return null;
                    }
                    case app.bpartners.geojobs.endpoint.rest.model.MultiPolygon multiPolygon -> {
                      return geometryConverter.apply(multiPolygon.getCoordinates());
                    }
                    case app.bpartners.geojobs.endpoint.rest.model.Polygon polygon -> {
                      return geometryConverter.apply(List.of(polygon.getCoordinates()));
                    }
                    default ->
                        throw new UnsupportedOperationException(
                            "Unsupported geometry instance during sync processing detection : "
                                + feature.getGeometry().getActualInstance());
                  }
                })
            .filter(Objects::nonNull)
            .reduce(unifyMultiPolygon())
            .orElseThrow(() -> new IllegalStateException("No provided geojson zone found"));

    var tileDetectionTasks =
        tilingTasks.stream()
            .map(
                task ->
                    task.getTiles().stream()
                        .filter(
                            tile -> {
                              var tileCoordinate = tile.getCoordinates();
                              var lonLatMultiPolygonFromTile =
                                  geometryConverter.getMultiPolygonFromTile(
                                      tileCoordinate.getX(),
                                      tileCoordinate.getY(),
                                      tileCoordinate.getZ());
                              return providedLonLatJtsMultiPolygon.intersects(
                                  lonLatMultiPolygonFromTile);
                            })
                        .map(
                            tile -> {
                              TileDetectionTask tileDetectionTask =
                                  new TileDetectionTask(
                                      null, null, null, null, tile, new ArrayList<>());
                              tileDetectionTask.setZoneDetectionJobId(zoneDetectionJob.getId());
                              tileDetectionTask.setDetectableObjectConfigurations(
                                  detection.getDetectableObjectConfigurations());
                              return tileDetectionTask;
                            })
                        .toList())
            .flatMap(List::stream)
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
