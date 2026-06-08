package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponseV2;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.utils.detection.SpecificDetectionTaskCreator;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ParcelDetectionTaskConsumerIT extends FacadeIT {
  private final String jobId;
  private final String detectionTaskId;
  private final String parcelId;
  @MockBean TileObjectDetector objectDetector;
  @MockBean DetectionMapper detectionMapper;
  @MockBean EventProducer eventProducer;
  @MockBean TileDetectionTaskRepository tileDetectionTaskRepository;
  @Autowired ParcelDetectionTaskConsumer subject;
  @Autowired DetectableObjectConfigurationRepository objectConfigurationRepository;
  @Autowired ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  @Autowired ZoneDetectionJobRepository jobRepository;
  @Autowired ParcelRepository parcelRepository;
  SpecificDetectionTaskCreator specificDetectionTaskCreator = new SpecificDetectionTaskCreator();

  ParcelDetectionTaskConsumerIT() {
    this.jobId = randomUUID().toString();
    this.detectionTaskId = randomUUID().toString();
    this.parcelId = randomUUID().toString();
  }

  private ParcelDetectionTask detectionTask() {
    List<Parcel> parcels = getParcels();
    return ParcelDetectionTask.builder()
        .id(detectionTaskId)
        .jobId(jobId)
        .parcels(parcels)
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .progression(PENDING)
                    .jobType(DETECTION)
                    .health(UNKNOWN)
                    .build()))
        .submissionInstant(Instant.now())
        .build();
  }

  @NotNull
  private List<Parcel> getParcels() {
    return List.of(
        Parcel.builder()
            .id(parcelId)
            .parcelContent(ParcelContent.builder().tiles(List.of(new Tile())).build())
            .build());
  }

  @BeforeEach
  void setUp() {
    when(objectDetector.apply(any(), any(), any()))
        .thenReturn(DetectionResponseV2.builder().build());
    when(detectionMapper.toDetectedTile(any(), any(), any(), any(), any()))
        .thenReturn(new MachineDetectedTile());
    when(tileDetectionTaskRepository.saveAll(any())).thenReturn(List.of(new TileDetectionTask()));
    jobRepository.save(ZoneDetectionJob.builder().id(jobId).build());
    parcelRepository.saveAll(getParcels());
    parcelDetectionTaskRepository.save(detectionTask());
    objectConfigurationRepository.save(
        DetectableObjectConfiguration.builder()
            .id(randomUUID().toString())
            .minConfidenceForDetection(0.70)
            .objectType(DetectableType.TOITURE_REVETEMENT)
            .detectionJobId(jobId)
            .build());
  }

  @Test
  void accept_ok() {
    subject.accept(
        specificDetectionTaskCreator.createPendingTask(
            jobId, detectionTaskId, parcelId, randomUUID().toString(), randomUUID().toString()));

    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(detectionTask().getParcels().size()))
        .accept(eventsCaptor.capture());
  }
}
