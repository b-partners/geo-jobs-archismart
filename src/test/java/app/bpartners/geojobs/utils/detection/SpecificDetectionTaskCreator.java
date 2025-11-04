package app.bpartners.geojobs.utils.detection;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import java.util.List;

public class SpecificDetectionTaskCreator {

  public ParcelDetectionTask createPendingTask(
      String jobId, String taskId, String parcelId, String parcelContentId, String tileId) {
    return ParcelDetectionTask.builder()
        .id(taskId)
        .jobId(jobId)
        .parcels(List.of(someParcel(parcelId, parcelContentId, tileId)))
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .progression(PENDING)
                    .jobType(DETECTION)
                    .health(UNKNOWN)
                    .build()))
        .build();
  }

  private Parcel someParcel(String parcelId, String parcelContentId, String tileId) {
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
