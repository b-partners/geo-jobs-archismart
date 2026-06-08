package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.ParcelRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionResponseV2;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class ParcelDetectionTaskCreatedServiceIT extends FacadeIT {
  private static final String FILE_NAME =
      "src"
          + File.separator
          + "test"
          + File.separator
          + "resources"
          + File.separator
          + "mockData"
          + File.separator
          + "image-to-detect.jpg";
  @Autowired ParcelRepository parcelRepository;
  @Autowired ParcelDetectionTaskCreatedService subject;
  @MockBean TileObjectDetector objectsDetector;
  @MockBean BucketComponent bucketComponent;
  @MockBean MachineDetectedTileRepository machineDetectedTileRepository;
  @MockBean ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  @MockBean ZoneDetectionJobService zoneDetectionJobService;
  @MockBean EventProducer eventProducer;
  List<Parcel> parcels = null;

  @BeforeEach
  void setUp() {
    parcels = parcelRepository.saveAll(parcels());
  }

  DetectionResponseV2 detectionResponse() {
    return DetectionResponseV2.builder()
        .images(
            Map.of(
                "filename",
                DetectionResponseV2.ImageData.builder()
                    .regions(
                        Map.of(
                            "0",
                            DetectionResponseV2.ImageData.Region.builder()
                                .shapeAttributes(
                                    DetectionResponseV2.ImageData.ShapeAttributes.builder()
                                        .allPointsX(
                                            List.of(
                                                new BigDecimal("210.6243386243386"),
                                                new BigDecimal("216.38095238095238"),
                                                new BigDecimal("223.83068783068782")))
                                        .allPointsY(
                                            List.of(
                                                new BigDecimal("220.78306878306876"),
                                                new BigDecimal("195.38624338624336"),
                                                new BigDecimal("210.6243386243386")))
                                        .build())
                                .regionAttributes(Map.of("label", "roof", "confidence", "0.8436"))
                                .build(),
                            "1",
                            DetectionResponseV2.ImageData.Region.builder()
                                .shapeAttributes(
                                    DetectionResponseV2.ImageData.ShapeAttributes.builder()
                                        .allPointsX(
                                            List.of(
                                                new BigDecimal("210.6243386243386"),
                                                new BigDecimal("216.38095238095238"),
                                                new BigDecimal("223.83068783068782")))
                                        .allPointsY(
                                            List.of(
                                                new BigDecimal("220.78306878306876"),
                                                new BigDecimal("195.38624338624336"),
                                                new BigDecimal("210.6243386243386")))
                                        .build())
                                .regionAttributes(Map.of("label", "roof", "confidence", "0.9612"))
                                .build()))
                    .build()))
        .build();
  }

  ParcelDetectionTaskCreated detectionTaskCreated() {
    String taskId = randomUUID().toString();
    String jobId = randomUUID().toString();

    return new ParcelDetectionTaskCreated(
        ParcelDetectionTask.builder()
            .id(taskId)
            .jobId(jobId)
            .parcels(parcels)
            .statusHistory(
                new ArrayList<>() {
                  {
                    TaskStatus.builder()
                        .id(randomUUID().toString())
                        .progression(PENDING)
                        .health(UNKNOWN)
                        .jobType(DETECTION)
                        .taskId(taskId)
                        .creationDatetime(now())
                        .build();
                  }
                })
            .build());
  }

  @NonNull
  private List<Parcel> parcels() {
    return List.of(
        Parcel.builder()
            .id(randomUUID().toString())
            .parcelContent(
                ParcelContent.builder()
                    .id(randomUUID().toString())
                    .tiles(
                        List.of(
                            Tile.builder()
                                .id(randomUUID().toString())
                                .coordinates(new TileCoordinates().x(25659).y(15466).z(20))
                                .bucketPath("mock-bucket-key")
                                .build()))
                    .build())
            .build());
  }

  ZoneDetectionJob zoneDetectionJob() {
    var task = detectionTaskCreated().getTask();
    String jobId = task.getJobId();
    return ZoneDetectionJob.builder()
        .id(jobId)
        .zoneName("mock")
        .emailReceiver("mock@gmail.com")
        .build();
  }

  @Test
  void process_detection() {
    when(machineDetectedTileRepository.save(any())).thenReturn(new MachineDetectedTile());
    when(bucketComponent.download(any())).thenReturn(new File(FILE_NAME));
    when(objectsDetector.apply(any(), any(), any())).thenReturn(detectionResponse());
    when(zoneDetectionJobService.findById(any())).thenReturn(zoneDetectionJob());
    when(parcelDetectionTaskRepository.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    subject.accept(detectionTaskCreated());

    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(1)).accept(eventsCaptor.capture());
  }
}
