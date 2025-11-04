package app.bpartners.geojobs.endpoint.rest.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.UNKNOWN;
import static app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.PENDING;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionTaskMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectedObject;
import app.bpartners.geojobs.endpoint.rest.model.DetectedParcel;
import app.bpartners.geojobs.endpoint.rest.model.DetectedTile;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.endpoint.rest.model.TileInfo;
import app.bpartners.geojobs.endpoint.rest.model.TileInfoSize;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.ParcelTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class DetectionTaskMapperTest {
  MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  DetectionTaskMapper subject = new DetectionTaskMapper(machineDetectedTileRepositoryMock);

  @Test
  void map_with_detected_tile_ok() {
    var parcelId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var tile =
        Tile.builder()
            .id(randomUUID().toString())
            .size(new TileInfoSize().height(1024).width(1024))
            .coordinates(new TileCoordinates().x(1234).y(5678).z(20))
            .creationDatetime(now())
            .build();

    MachineDetectedTile machineDetectedTile =
        MachineDetectedTile.builder()
            .tile(tile)
            .detectedObjects(
                List.of(
                    someDetectedObject(PANNEAU_PHOTOVOLTAIQUE),
                    someDetectedObject(TOITURE_REVETEMENT),
                    someDetectedObject(ARBRE),
                    someDetectedObject(PISCINE),
                    someDetectedObject(PASSAGE_PIETON)))
            .creationDatetime(now())
            .build();
    when(machineDetectedTileRepositoryMock.findAllByParcelId(parcelId))
        .thenReturn(List.of(machineDetectedTile));

    DetectedParcel actual = subject.toRest(jobId, someParcelTask(parcelId));

    var status =
        new Status()
            .creationDatetime(actual.getCreationDatetime())
            .progression(PENDING)
            .health(UNKNOWN);
    assertEquals(
        new DetectedParcel()
            .id(actual.getId())
            .detectionJobIb(actual.getDetectionJobIb())
            .parcelId(parcelId)
            .status(status)
            .creationDatetime(actual.getCreationDatetime())
            .detectedTiles(
                List.of(
                    new DetectedTile()
                        .tileId(tile.getId())
                        .tileInfo(
                            new TileInfo().size(tile.getSize()).coordinates(tile.getCoordinates()))
                        .creationDatetime(tile.getCreationDatetime())
                        .status(status)
                        .detectedObjects(
                            List.of(
                                someRestDetectedObject(
                                    app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType
                                        .PANNEAU_PHOTOVOLTAIQUE),
                                someRestDetectedObject(
                                    app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType
                                        .TOITURE_REVETEMENT),
                                someRestDetectedObject(
                                    app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType
                                        .ARBRE),
                                someRestDetectedObject(
                                    app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType
                                        .PISCINE),
                                someRestDetectedObject(
                                    app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType
                                        .PASSAGE_PIETON))))),
        actual);
  }

  private ParcelTask someParcelTask(String parcelId) {
    return ParcelTask.builder()
        .parcel(Parcel.builder().id(parcelId).parcelContent(mock(ParcelContent.class)).build())
        .status(null)
        .build();
  }

  @Test
  void map_detected_tile_with_all_detectable_objects_type_ok() {
    when(machineDetectedTileRepositoryMock.findAllByParcelId(any()))
        .thenReturn(
            List.of(
                MachineDetectedTile.builder()
                    .tile(Tile.builder().build())
                    .detectedObjects(detectedObjectWithAllTypes())
                    .build()));

    var actual = subject.toRest(randomUUID().toString(), someParcelTask(randomUUID().toString()));

    var restDetectableObjectTypes =
        Objects.requireNonNull(actual.getDetectedTiles()).stream()
            .map(
                detectedTile ->
                    Objects.requireNonNull(detectedTile.getDetectedObjects()).stream()
                        .map(DetectedObject::getDetectedObjectType)
                        .filter(Objects::nonNull)
                        .toList())
            .flatMap(List::stream)
            .toList();
    // TODO: uncomment when type TOMB is handled
    // assertEquals(restDetectableObjectTypes().size(), restDetectableObjectTypes.size());
    assertTrue(restDetectableObjectTypes().containsAll(restDetectableObjectTypes));
  }

  private List<app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType>
      restDetectableObjectTypes() {
    return Arrays.asList(app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.values());
  }

  private List<app.bpartners.geojobs.repository.model.detection.DetectedObject>
      detectedObjectWithAllTypes() {
    var detectableTypes = Arrays.asList(DetectableType.values());
    return detectableTypes.stream()
        .map(
            detectableType ->
                app.bpartners.geojobs.repository.model.detection.DetectedObject.builder()
                    .computedConfidence(1.0) // Not nullable
                    .detectedObjectType(
                        DetectableObjectType.builder().detectableType(detectableType).build())
                    .build())
        .toList();
  }

  private static DetectedObject someRestDetectedObject(
      app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType restObjectType) {
    return new DetectedObject()
        .confidence(BigDecimal.valueOf(1.0))
        .detectedObjectType(restObjectType)
        .detectorVersion("TODO");
  }

  private app.bpartners.geojobs.repository.model.detection.DetectedObject someDetectedObject(
      DetectableType detectableType) {
    return app.bpartners.geojobs.repository.model.detection.DetectedObject.builder()
        .detectedObjectType(DetectableObjectType.builder().detectableType(detectableType).build())
        .computedConfidence(1.0)
        .build();
  }
}
