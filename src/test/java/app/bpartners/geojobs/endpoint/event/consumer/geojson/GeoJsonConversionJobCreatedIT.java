package app.bpartners.geojobs.endpoint.event.consumer.geojson;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;
import static app.bpartners.geojobs.file.hash.FileHashAlgorithm.SHA256;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.model.page.BoundedPageSize.MAX_SIZE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE;
import static java.time.Instant.now;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.rest.model.GeoJsonsUrl;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.sqs.EventProducerInvocationMock;
import app.bpartners.geojobs.sqs.LocalEventQueue;
import app.bpartners.geojobs.utils.detection.DetectionIT;
import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class GeoJsonConversionJobCreatedIT extends DetectionIT {
  private static final int DEFAULT_EVENT_DELAY_SPEED_FACTOR = 10;
  private static final double DEFAULT_COMPUTED_CONFIDENCE = 0.9;
  EventProducerInvocationMock eventProducerInvocationMock = new EventProducerInvocationMock();
  @Autowired LocalEventQueue localEventQueue;
  @Autowired GeoJsonConversionJobService geoJsonConversionJobService;
  @Autowired ZoneDetectionJobRepository zoneDetectionJobRepository;
  @MockBean EventProducer eventProducerMock;
  @MockBean BucketComponent bucketComponentMock;
  @Autowired MachineDetectedTileRepository machineDetectedTileRepository;
  @Autowired GeoJsonConversionJobRepository geoJsonConversionJobRepository;
  @Autowired DetectableObjectConfigurationRepository objectConfigurationRepository;

  @BeforeEach
  void setUp() {
    localEventQueue.configure(customEventConfigList(), DEFAULT_EVENT_DELAY_SPEED_FACTOR);
    doAnswer(
            invocationOnMock ->
                eventProducerInvocationMock.apply(localEventQueue, invocationOnMock))
        .when(eventProducerMock)
        .accept(any());

    when(bucketComponentMock.upload(any(), any()))
        .thenReturn(new FileHash(SHA256, randomUUID().toString()));
    when(bucketComponentMock.download(any())).thenReturn(dummyGeoFeaturesFile());
  }

  @SneakyThrows
  private File dummyGeoFeaturesFile() {
    File tempFile = File.createTempFile(randomUUID().toString(), ".geojson");
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write(
          """
          [
            {
              "properties": {},
              "geometry": {
              "type": "MultiPolygon",
              "coordinates": [[[[0,0]]]]
              },
              "type": "Feature"
            }
          ]""");
    }
    return tempFile;
  }

  @NonNull
  private static List<LocalEventQueue.CustomEventDelayConfig> customEventConfigList() {
    return List.of(
        new LocalEventQueue.CustomEventDelayConfig(
            GeoJsonConversionJobStatusRecomputingSubmitted.class, 50));
  }

  @Test
  void create_geo_json_conversion_ko() {
    var zoneDetectionJobId = randomUUID().toString();

    assertThrows(
        NotFoundException.class,
        () -> geoJsonConversionJobService.getOrComputeGeoJsonUrl(zoneDetectionJobId));
  }

  @NonNull
  private ZoneDetectionJob randomMachineZDJ() {
    var dummyZone = "dummyZone";
    var emailReceiver = "dummy@email.com";
    var ztj =
        zoneTilingJobCreator.create(
            randomUUID().toString(), dummyZone, emailReceiver, FINISHED, SUCCEEDED);
    var zdj =
        zoneDetectionJobCreator.create(
            randomUUID().toString(), dummyZone, emailReceiver, FINISHED, SUCCEEDED, ztj);
    return zoneDetectionJobRepository.save(zdj);
  }

  @Test
  void create_geo_json_conversion() {
    var zoneDetectionJob = randomMachineZDJ();
    var zoneDetectionJobId = zoneDetectionJob.getId();

    var actualGeoJsonUrl = geoJsonConversionJobService.getOrComputeGeoJsonUrl(zoneDetectionJobId);

    var eventListCaptor = ArgumentCaptor.forClass(List.class);
    assertEquals(
        new GeoJsonsUrl()
            .url(null)
            .status(
                new Status()
                    .progression(Status.ProgressionEnum.FINISHED)
                    .health(Status.HealthEnum.SUCCEEDED)
                    .creationDatetime(actualGeoJsonUrl.getStatus().getCreationDatetime())),
        actualGeoJsonUrl);
    verify(eventProducerMock, times(1)).accept(eventListCaptor.capture());
    var geoJsonConversionJobCreated =
        (GeoJsonConversionJobCreated) eventListCaptor.getValue().getFirst();
    var geoJsonConversionJob = geoJsonConversionJobCreated.getGeoJsonConversionJob();
    assertEquals(
        GeoJsonConversionJob.builder()
            .id(geoJsonConversionJob.getId())
            .zoneDetectionJobId(zoneDetectionJobId)
            .zoneDetectionJobType(zoneDetectionJob.getDetectionType())
            .submissionInstant(geoJsonConversionJob.getSubmissionInstant())
            .statusHistory(geoJsonConversionJob.getStatusHistory())
            .build(),
        geoJsonConversionJob);
    assertEquals(Duration.ofMinutes(5L), geoJsonConversionJobCreated.maxConsumerDuration());
    assertEquals(
        Duration.ofMinutes(1L), geoJsonConversionJobCreated.maxConsumerBackoffBetweenRetries());
    assertEquals(EVENT_STACK_2, geoJsonConversionJobCreated.getEventStack());
  }

  @SneakyThrows
  @Test
  void create_geo_json_conversion_and_get_geo_json_url_ok() {
    var zoneDetectionJob = randomMachineZDJ();
    var zoneDetectionJobId = zoneDetectionJob.getId();
    objectConfigurationRepository.saveAll(
        List.of(
            DetectableObjectConfiguration.builder()
                .id(randomUUID().toString())
                .detectionJobId(zoneDetectionJobId)
                .objectType(ARBRE)
                .build()));
    machineDetectedTileRepository.saveAll(
        someMachineDetectedTile(MAX_SIZE + 1, zoneDetectionJobId));

    var actualGeoJsonUrl = geoJsonConversionJobService.getOrComputeGeoJsonUrl(zoneDetectionJobId);

    assertEquals(
        new GeoJsonsUrl()
            .url(null)
            .status(
                new Status()
                    .progression(Status.ProgressionEnum.FINISHED)
                    .health(Status.HealthEnum.SUCCEEDED)
                    .creationDatetime(actualGeoJsonUrl.getStatus().getCreationDatetime())),
        actualGeoJsonUrl);

    Thread.sleep(Duration.ofSeconds(30L));
    if (localEventQueue != null) localEventQueue.attemptSchedulerShutDown();

    var actualGeoJsonConversionJob =
        geoJsonConversionJobRepository.findByZoneDetectionJobId(zoneDetectionJobId).stream()
            .sorted(
                comparing(GeoJsonConversionJob::getSubmissionInstant, naturalOrder()).reversed())
            .toList()
            .getFirst();
    assertNotNull(actualGeoJsonConversionJob.getFileKey());
  }

  MachineDetectedTile randomMachineDetectedTile(
      String detectedTileId, String zoneDetectionJobId, List<DetectedObject> detectedObjects) {
    var bucketPath = "dummyBucketPath";
    var dummyTile =
        Tile.builder()
            .id(detectedTileId)
            .coordinates(new TileCoordinates().x(10).y(20).z(30))
            .bucketPath(bucketPath)
            .creationDatetime(now())
            .build();
    return MachineDetectedTile.builder()
        .id(detectedTileId)
        .tile(dummyTile)
        .zdjJobId(zoneDetectionJobId)
        .detectedObjects(detectedObjects)
        .creationDatetime(now())
        .bucketPath(bucketPath)
        .build();
  }

  DetectedObject someDetectedObject(String detectedTile, DetectableType detectableType) {
    var detectedObjectId = randomUUID().toString();
    return DetectedObject.builder()
        .id(detectedObjectId)
        .detectedTileId(detectedTile)
        .detectedObjectType(
            DetectableObjectType.builder()
                .id(randomUUID().toString())
                .detectableType(detectableType)
                .objectId(detectedObjectId)
                .build())
        .computedConfidence(DEFAULT_COMPUTED_CONFIDENCE)
        .feature(new Feature())
        .build();
  }

  List<MachineDetectedTile> someMachineDetectedTile(Integer nb, String zoneDetectionJobId) {
    var list = new ArrayList<MachineDetectedTile>();
    for (int i = 0; i < nb; i++) {
      var detectedTileId = randomUUID().toString();
      list.add(
          randomMachineDetectedTile(
              detectedTileId,
              zoneDetectionJobId,
              List.of(someDetectedObject(detectedTileId, ARBRE))));
    }
    return list;
  }
}
