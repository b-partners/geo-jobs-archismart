package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionJobCreated;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.KeyPredicateFunction;
import app.bpartners.geojobs.service.TaskConsumer;
import app.bpartners.geojobs.service.TaskToJobConverter;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class ParcelDetectionTaskConsumer implements TaskConsumer<ParcelDetectionTask> {
  private final EventProducer eventProducer;
  private final TaskToJobConverter<ParcelDetectionTask, ParcelDetectionJob> taskToJobConverter;
  private final ParcelDetectionJobService parcelDetectionJobService;
  private final KeyPredicateFunction keyPredicateFunction;
  private final ParcelDetectionTaskRepository parcelDetectionTaskRepository;

  @Override
  public void accept(ParcelDetectionTask task) {
    String detectionTaskId = task.getId();
    String zdjId = task.getJobId();
    if (task.getParcels().isEmpty()) {
      throw new IllegalArgumentException(
          "DetectionTask(id=" + detectionTaskId + ", zdjId=" + zdjId + ") does not have parcel");
    } else if (task.getParcels().stream()
        .anyMatch(parcel -> parcel.getParcelContent().getTiles().isEmpty())) {
      throw new IllegalArgumentException(
          "DetectionTask(id="
              + detectionTaskId
              + ", zdjId="
              + zdjId
              + ") has parcel without tiles");
    }
    log.info("Processing parcel detection task {}", task);
    ParcelDetectionJob parcelDetectionJob = taskToJobConverter.apply(task);
    log.info("Processing parcel detection job {}", parcelDetectionJob);
    task.setAsJobId(parcelDetectionJob.getId());
    ParcelDetectionTask savedParcelTask = parcelDetectionTaskRepository.save(task);
    List<TileDetectionTask> tileDetectionTasks =
        savedParcelTask.getTiles().stream()
            .filter(keyPredicateFunction.apply(Tile::getBucketPath))
            .map(tile -> taskToJobConverter.apply(savedParcelTask, tile))
            .toList();
    ParcelDetectionJob createdParcelDetectionJob =
        parcelDetectionJobService.create(parcelDetectionJob, tileDetectionTasks);
    eventProducer.accept(
        List.of(
            ParcelDetectionJobCreated.builder()
                .zdjId(zdjId)
                .parcelDetectionJob(createdParcelDetectionJob)
                .build()));
  }
}
