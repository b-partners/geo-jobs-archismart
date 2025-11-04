package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.model.AnnotationBatch;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.HumanDetectedTileRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingTask;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AnnotationRetrievingTaskConsumer implements Consumer<AnnotationRetrievingTask> {
  private final AnnotationService annotationService;
  private final HumanDetectedTileRepository humanDetectedTileRepository;
  private final DetectionMapper detectionMapper;

  @Override
  public void accept(AnnotationRetrievingTask task) {
    var annotationBatches =
        annotationService.getAnnotations(task.getAnnotationJobId(), task.getAnnotationTaskId());
    var humanDetectedTiles =
        annotationBatches.stream()
            .map(AnnotationBatch::getAnnotations)
            .filter(Objects::nonNull)
            .map(
                annotations -> {
                  var tileId = randomUUID().toString();
                  return HumanDetectedTile.builder()
                      .id(tileId)
                      .jobId(task.getHumanZoneDetectionJobId())
                      .annotationJobId(task.getAnnotationJobId())
                      .machineDetectedTileId(task.getAnnotationTaskId())
                      .imageSize(1024)
                      .detectedObjects(
                          detectionMapper.toHumanDetectedObject(
                              task.getZoom(), tileId, annotations))
                      .tile(
                          Tile.builder()
                              .id(randomUUID().toString())
                              .creationDatetime(now())
                              .coordinates(
                                  new TileCoordinates()
                                      .x(task.getXTile())
                                      .y(task.getYTile())
                                      .z(task.getZoom()))
                              .build())
                      .build();
                })
            .toList();
    humanDetectedTileRepository.saveAll(humanDetectedTiles);
  }

  public static AnnotationRetrievingTask withNewStatus(
      AnnotationRetrievingTask task,
      Status.ProgressionStatus progression,
      Status.HealthStatus health,
      String message) {
    return (AnnotationRetrievingTask)
        task.hasNewStatus(
            Status.builder()
                .progression(progression)
                .health(health)
                .creationDatetime(now())
                .message(message)
                .build());
  }
}
