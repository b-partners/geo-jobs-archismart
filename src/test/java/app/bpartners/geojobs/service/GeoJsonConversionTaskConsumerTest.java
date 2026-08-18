package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE_INTENSE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE_IMPORTANTE;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.postprocessing.StationnementPostProcessor;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.HumanDetectedTileRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.io.File;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GeoJsonConversionTaskConsumerTest {
  private static final String ZONE_DETECTION_JOB_ID = "zoneDetectionJobId";
  private static final String CONVERSION_JOB_ID = "conversionJobId";
  MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  HumanDetectedTileRepository humanDetectedTileRepositoryMock = mock();
  GeoJsonConversionJobRepository geoJsonConversionJobRepositoryMock = mock();
  GeoJsonConverter geoJsonConverterMock = mock();
  BucketComponent bucketComponentMock = mock();
  FileWriter fileWriterMock = mock();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  StationnementPostProcessor stationnementPostProcessor =
      new StationnementPostProcessor(new GeometryConverter(), new GeometrySquareMeterArea());
  GeoJsonConversionTaskConsumer subject =
      new GeoJsonConversionTaskConsumer(
          machineDetectedTileRepositoryMock,
          humanDetectedTileRepositoryMock,
          geoJsonConversionJobRepositoryMock,
          geoJsonConverterMock,
          bucketComponentMock,
          fileWriterMock,
          zoneDetectionJobServiceMock,
          detectionRepositoryMock,
          stationnementPostProcessor);

  @BeforeEach
  void setUp() {
    when(detectionRepositoryMock.findByZdjId(any())).thenReturn(Optional.empty());
  }

  @Test
  void consumes_human_detected_tiles_without_exceptions() {
    when(geoJsonConversionJobRepositoryMock.findById(any()))
        .thenReturn(
            Optional.of(
                GeoJsonConversionJob.builder()
                    .id(CONVERSION_JOB_ID)
                    .zoneDetectionJobType(HUMAN)
                    .zoneDetectionJobId(ZONE_DETECTION_JOB_ID)
                    .build()));
    when(zoneDetectionJobServiceMock.findById(ZONE_DETECTION_JOB_ID))
        .thenReturn(
            ZoneDetectionJob.builder().id(ZONE_DETECTION_JOB_ID).zoneName("dummyZoneName").build());
    when(humanDetectedTileRepositoryMock.findAllByJobId(any(), any()))
        .thenReturn(List.of(humanDetectedTile()));
    when(geoJsonConverterMock.apply(any(), any())).thenReturn(new GeoJson(List.of()));
    when(fileWriterMock.writeAsByte(any())).thenReturn(null);
    when(fileWriterMock.write(any(), any(), any())).thenReturn(mock(File.class));
    when(bucketComponentMock.upload(any(), any())).thenReturn(mock(FileHash.class));

    assertDoesNotThrow(
        () ->
            subject.accept(
                GeoJsonConversionTask.builder()
                    .page(1)
                    .detectableType(HUMIDITE_INTENSE)
                    .jobId(CONVERSION_JOB_ID)
                    .build()));
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(humanDetectedTileRepositoryMock, only()).findAllByJobId(any(), any());
    verify(machineDetectedTileRepositoryMock, never()).findAllByZdjJobId(any(), any());
    verify(geoJsonConverterMock, only()).apply(listCaptor.capture(), any());
    var detectedTile = (DetectedTile) listCaptor.getValue().getFirst();
    assertEquals(
        expectedDetectedTile(detectedTile.getDetectedObjects().getFirst().getId()), detectedTile);
  }

  @Test
  void consumes_machine_detected_tiles_without_exceptions() {
    when(geoJsonConversionJobRepositoryMock.findById(any()))
        .thenReturn(
            Optional.of(
                GeoJsonConversionJob.builder()
                    .id(CONVERSION_JOB_ID)
                    .zoneDetectionJobType(MACHINE)
                    .zoneDetectionJobId(ZONE_DETECTION_JOB_ID)
                    .build()));
    when(zoneDetectionJobServiceMock.findById(ZONE_DETECTION_JOB_ID))
        .thenReturn(
            ZoneDetectionJob.builder().id(ZONE_DETECTION_JOB_ID).zoneName("dummyZoneName").build());
    when(machineDetectedTileRepositoryMock.findAllByZdjJobIdAndDetectableType(any(), any(), any()))
        .thenReturn(
            List.of(machineDetectedTile(HUMIDITE_INTENSE), machineDetectedTile(USURE_IMPORTANTE)));
    when(geoJsonConverterMock.apply(any(), any())).thenReturn(new GeoJson(List.of()));
    when(fileWriterMock.writeAsByte(any())).thenReturn(null);
    when(fileWriterMock.write(any(), any(), any())).thenReturn(mock(File.class));
    when(bucketComponentMock.upload(any(), any())).thenReturn(mock(FileHash.class));

    assertDoesNotThrow(
        () ->
            subject.accept(
                GeoJsonConversionTask.builder()
                    .page(1)
                    .jobId(CONVERSION_JOB_ID)
                    .detectableType(HUMIDITE_INTENSE)
                    .build()));
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(humanDetectedTileRepositoryMock, never()).findAllByJobId(any(), any());
    verify(machineDetectedTileRepositoryMock, only())
        .findAllByZdjJobIdAndDetectableType(any(), any(), any());
    verify(geoJsonConverterMock, only()).apply(listCaptor.capture(), any());
    var detectedTile = (DetectedTile) listCaptor.getValue().getFirst();
    assertEquals(
        expectedDetectedTile(detectedTile.getDetectedObjects().getFirst().getId()), detectedTile);
  }

  @Test
  void consumes_ko() {
    var conversionJobId = "conversionJobId";
    when(geoJsonConversionJobRepositoryMock.findById(conversionJobId)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> subject.accept(GeoJsonConversionTask.builder().jobId(conversionJobId).build()));
  }

  private static DetectedTile expectedDetectedTile(String detectedObjectId) {
    return DetectedTile.builder()
        .tile(humanDetectedTile().getTile())
        .detectedObjects(
            List.of(
                DetectedObject.builder()
                    .id(detectedObjectId)
                    .computedConfidence(0.9)
                    .detectedObjectType(
                        DetectableObjectType.builder().detectableType(HUMIDITE_INTENSE).build())
                    .feature(
                        Feature.builder()
                            .id("feature_id")
                            .zoom(20)
                            .geometry(
                                Feature.FeatureGeometry.builder()
                                    .geometryType(POLYGON)
                                    .actualInstanceStringValue("{}")
                                    .build())
                            .build())
                    .build()))
        .build();
  }

  private static HumanDetectedTile humanDetectedTile() {
    return HumanDetectedTile.builder()
        .tile(new Tile())
        .detectedObjects(
            List.of(
                DetectedObject.builder()
                    .detectedObjectType(
                        DetectableObjectType.builder().detectableType(HUMIDITE_INTENSE).build())
                    .computedConfidence(0.9)
                    .feature(
                        Feature.builder()
                            .id("feature_id")
                            .zoom(20)
                            .geometry(
                                Feature.FeatureGeometry.builder()
                                    .geometryType(POLYGON)
                                    .actualInstanceStringValue("{}")
                                    .build())
                            .build())
                    .build()))
        .build();
  }

  private MachineDetectedTile machineDetectedTile(DetectableType detectableType) {
    return MachineDetectedTile.builder()
        .tile(new Tile())
        .detectedObjects(List.of(detectedObjectBuilder(detectableType)))
        .build();
  }

  private DetectedObject detectedObjectBuilder(DetectableType detectableType) {
    return DetectedObject.builder()
        .id(randomUUID().toString())
        .detectedObjectType(DetectableObjectType.builder().detectableType(detectableType).build())
        .computedConfidence(0.9)
        .feature(
            Feature.builder()
                .id("feature_id")
                .zoom(20)
                .geometry(
                    Feature.FeatureGeometry.builder()
                        .geometryType(POLYGON)
                        .actualInstanceStringValue("{}")
                        .build())
                .build())
        .build();
  }
}
