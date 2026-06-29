package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.*;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionFileObjectRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.DetectionFileObject;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.*;
import app.bpartners.geojobs.service.detection.*;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TileDetectionTaskConsumerTest {
  MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  TileObjectDetector objectDetectorMock = mock();
  DetectionMapper detectionMapperMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  GeometryConverter geometryConverterMock = mock();
  DetectionMaskFromTileRetriever maskRetrieverMock = mock();
  RoofCoveringDetector roofCoveringDetectorMock = mock();
  BucketComponent bucketComponentMock = mock();
  DetectionFileObjectRepository detectionObjectHistoryRepositoryMock = mock();
  TileCoordinatesPolygonIntersection tileCoordinatesPolygonIntersectionMock = mock();
  FilePolygonDrawer filePolygonDrawerMock = mock();
  DetectedTileVggExtractor detectedTileVggExtractorMock = mock();
  VggImageAnnotator vggImageAnnotatorMock = mock();

  TileDetectionTaskConsumer subject =
      new TileDetectionTaskConsumer(
          machineDetectedTileRepositoryMock,
          objectDetectorMock,
          detectionMapperMock,
          detectionRepositoryMock,
          geometryConverterMock,
          maskRetrieverMock,
          roofCoveringDetectorMock,
          detectionObjectHistoryRepositoryMock,
          bucketComponentMock,
          tileCoordinatesPolygonIntersectionMock,
          filePolygonDrawerMock,
          detectedTileVggExtractorMock,
          vggImageAnnotatorMock);

  @Test
  void persist_detection_file_object_when_detection_debug_mode() {
    var detectionMock = mock(Detection.class);
    var detectableObjectConfigurations = List.of(mock(DetectableObjectConfiguration.class));
    var zoneDetectionJobId = randomUUID().toString();
    var parcelId = randomUUID().toString();
    var parcelJobId = randomUUID().toString();
    var detectionIdentifier = randomUUID().toString();
    var tileMock = mock(Tile.class);
    var featureMock = mock(Feature.class);
    var featureGeometryMock = mock(FeatureGeometry.class);
    var featureMultiPolygonMock = mock(MultiPolygon.class);
    var roofMultiPolygonMock = mock(org.locationtech.jts.geom.MultiPolygon.class);
    var maskFileMock = mock(File.class);
    var detectionResponseMock = mock(DetectionResponseV2.class);
    var machineDetectedTileMock = mock(MachineDetectedTile.class);
    var providedFeatureMockDomain = mock(app.bpartners.geojobs.repository.model.Feature.class);
    var roofDelimitationMockDomain = mock(app.bpartners.geojobs.repository.model.Feature.class);
    var roofFeatureGeometryMock =
        mock(app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.class);
    var tileDetectionTask =
        TileDetectionTask.builder()
            .parcelId(parcelId)
            .zoneDetectionJobId(zoneDetectionJobId)
            .detectableObjectConfigurations(detectableObjectConfigurations)
            .tile(tileMock)
            .jobId(parcelJobId)
            .build();
    var tileImageBucketPath = "layer/20/0/10/tileBucketPath.jpg";
    var tileMaskBucketPath = "layer/20/0/10/tileBucketPath_mask.jpg";
    var drawnTileBucketPath = "layer/20/0/10/tileBucketPath_drawn_tile.jpg";
    var tileDrawnMaskBucketPath = "layer/20/0/10/tileBucketPath_drawn_mask.jpg";
    var tileBuilderMock = mock(Tile.TileBuilder.class);
    when(tileBuilderMock.detectionE2Id(any())).thenReturn(tileBuilderMock);
    when(tileBuilderMock.build()).thenReturn(tileMock);
    when(tileMock.toBuilder()).thenReturn(tileBuilderMock);
    when(tileMock.getBucketPath()).thenReturn(tileImageBucketPath);
    when(tileMock.getCoordinates()).thenReturn(new TileCoordinates().x(0).y(0).z(20));
    when(roofDelimitationMockDomain.getGeometry()).thenReturn(mock());
    when(geometryConverterMock.apply(featureMultiPolygonMock.getCoordinates()))
        .thenReturn(roofMultiPolygonMock);
    when(featureGeometryMock.getMultiPolygon()).thenReturn(featureMultiPolygonMock);
    when(featureGeometryMock.getActualInstance()).thenReturn(featureMultiPolygonMock);
    when(featureMock.getGeometry()).thenReturn(featureGeometryMock);
    when(detectionMock.isDebugMode()).thenReturn(true);
    when(detectionMock.hasToitureModelName()).thenReturn(true);
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(List.of(featureMock));
    when(roofFeatureGeometryMock.getActualInstanceStringValue())
        .thenReturn("roofGeometryActualInstanceStringValue");
    var multiPolygonFromTileMock = mock(org.locationtech.jts.geom.MultiPolygon.class);
    when(roofDelimitationMockDomain.getGeometry()).thenReturn(roofFeatureGeometryMock);
    when(roofMultiPolygonMock.contains(eq(multiPolygonFromTileMock))).thenReturn(true);
    when(multiPolygonFromTileMock.intersection(roofMultiPolygonMock))
        .thenReturn(roofMultiPolygonMock);
    when(detectionMock.getId()).thenReturn(detectionIdentifier);
    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(
            List.of(
                new FeatureWithDelimitation(
                    providedFeatureMockDomain, List.of(roofDelimitationMockDomain))));
    when(detectionRepositoryMock.findByZdjId(zoneDetectionJobId))
        .thenReturn(Optional.of(detectionMock));
    when(geometryConverterMock.getMultiPolygonFromTile(0, 0, 20))
        .thenReturn(multiPolygonFromTileMock);
    when(geometryConverterMock.readGeometryFromString(eq("roofGeometryActualInstanceStringValue")))
        .thenReturn(roofMultiPolygonMock);
    when(roofMultiPolygonMock.intersection(any())).thenReturn(roofMultiPolygonMock);
    when(machineDetectedTileRepositoryMock.save(any())).thenReturn(machineDetectedTileMock);
    when(objectDetectorMock.apply(any(), any(), any())).thenReturn(detectionResponseMock);
    when(detectionMapperMock.toDetectedTile(any(), any(), any(), any(), any()))
        .thenReturn(new MachineDetectedTile());
    when(maskRetrieverMock.apply(tileMock, roofMultiPolygonMock)).thenReturn(maskFileMock);
    when(roofCoveringDetectorMock.apply(any(Tile.class), any(File.class)))
        .thenReturn(
            new RoofCoveringDetector.RoofCoveringDetectionResponse(
                new RoofCovering(RoofCoveringType.ROOF_ARDOISE, 1100L),
                new RoofCovering(RoofCoveringType.ROOF_TUILES, 1000L)));
    when(bucketComponentMock.upload(any(), any())).thenReturn(mock());
    when(bucketComponentMock.download(any())).thenReturn(mock());

    assertDoesNotThrow(() -> subject.accept(tileDetectionTask));

    var detectionFileObjectCaptor = ArgumentCaptor.forClass(DetectionFileObject.class);
    verify(detectionObjectHistoryRepositoryMock, times(4))
        .save(detectionFileObjectCaptor.capture());
    var actualSavedFileObject = detectionFileObjectCaptor.getAllValues();
    var savedDrawnMaskFileObject = actualSavedFileObject.getFirst();
    var savedFileObjectTile = actualSavedFileObject.get(1);
    var savedTileMaskFileObject = actualSavedFileObject.get(2);
    var savedAnnotatedTileFileObject = actualSavedFileObject.getLast();
    assertEquals(
        DetectionFileObject.builder()
            .id(savedDrawnMaskFileObject.getId())
            .detectionIdentifier(detectionIdentifier)
            .bucketKey(tileDrawnMaskBucketPath)
            .fileType(TILE_MASK)
            .fileName("layer_20_0_10_tileBucketPath_drawn_mask.jpg")
            .creationDatetime(savedDrawnMaskFileObject.getCreationDatetime())
            .build(),
        savedDrawnMaskFileObject);
    assertEquals(
        DetectionFileObject.builder()
            .id(savedFileObjectTile.getId())
            .detectionIdentifier(detectionIdentifier)
            .bucketKey(tileImageBucketPath)
            .fileType(TILE_IMAGE)
            .fileName("layer_20_0_10_tileBucketPath.jpg")
            .creationDatetime(savedFileObjectTile.getCreationDatetime())
            .build(),
        savedFileObjectTile);
    assertEquals(
        DetectionFileObject.builder()
            .id(savedTileMaskFileObject.getId())
            .detectionIdentifier(detectionIdentifier)
            .bucketKey(tileMaskBucketPath)
            .fileName("layer_20_0_10_tileBucketPath_mask.jpg")
            .fileType(TILE_MASK)
            .creationDatetime(savedTileMaskFileObject.getCreationDatetime())
            .build(),
        savedTileMaskFileObject);
    assertEquals(
        DetectionFileObject.builder()
            .id(savedAnnotatedTileFileObject.getId())
            .detectionIdentifier(detectionIdentifier)
            .bucketKey(drawnTileBucketPath)
            .fileName("layer_20_0_10_tileBucketPath_drawn_tile.jpg")
            .fileType(ANNOTATED_TILE_IMAGE)
            .creationDatetime(savedAnnotatedTileFileObject.getCreationDatetime())
            .build(),
        savedAnnotatedTileFileObject);
  }

  @Test
  void do_nothing_when_detection_has_toiture_model_but_without_mask() {
    var zoneDetectionJobId = randomUUID().toString();
    var tileMock = mock(Tile.class);
    var tileDetectionTask =
        TileDetectionTask.builder().zoneDetectionJobId(zoneDetectionJobId).tile(tileMock).build();
    var detectionMock = mock(Detection.class);
    var geometryActualInstanceStringValue = "geometryActualInstanceStringValue";
    var geometryMock =
        geometryFactory.createMultiPolygon(new org.locationtech.jts.geom.Polygon[] {});
    var multiPolygonFromTileMock = mock(org.locationtech.jts.geom.MultiPolygon.class);
    when(tileMock.getCoordinates()).thenReturn(new TileCoordinates().x(0).y(0).z(20));
    when(detectionMock.hasToitureModelName()).thenReturn(true);
    when(multiPolygonFromTileMock.intersection(geometryMock)).thenReturn(geometryMock);
    when(geometryConverterMock.readGeometryFromString(geometryActualInstanceStringValue))
        .thenReturn(geometryMock);
    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(
            List.of(
                new FeatureWithDelimitation(
                    mock(),
                    List.of(
                        app.bpartners.geojobs.repository.model.Feature.builder()
                            .geometry(
                                new app.bpartners.geojobs.repository.model.Feature.FeatureGeometry(
                                    Geometry.TypeEnum.POLYGON, geometryActualInstanceStringValue))
                            .build()))));
    when(geometryConverterMock.getMultiPolygonFromTile(anyInt(), anyInt(), anyInt()))
        .thenReturn(multiPolygonFromTileMock);
    when(detectionRepositoryMock.findByZdjId(zoneDetectionJobId))
        .thenReturn(Optional.of(detectionMock));
    when(maskRetrieverMock.apply(any(), any())).thenReturn(null);

    assertDoesNotThrow(() -> subject.accept(tileDetectionTask));

    verify(objectDetectorMock, never()).apply(any(), any(), any());
    verify(roofCoveringDetectorMock, never()).apply(any(), any());
    verify(detectionMapperMock, never()).toDetectedTile(any(), any(), any(), any(), any());
    verify(machineDetectedTileRepositoryMock, never()).save(any());
  }

  @Test
  void do_nothing_as_roof_polygon_not_intersecting_with_tile_polygon() {
    var detectableObjectConfigurations = List.of(mock(DetectableObjectConfiguration.class));
    var zoneDetectionJobId = randomUUID().toString();
    var parcelId = randomUUID().toString();
    var parcelJobId = randomUUID().toString();
    var tileMock = mock(Tile.class);
    var detectionMock = mock(Detection.class);
    var featureMock = mock(Feature.class);
    var featureGeometryMock = mock(FeatureGeometry.class);
    var featureMultiPolygonMock = mock(MultiPolygon.class);
    var centroidCoordinates = List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(1));
    var roofMultiPolygonMock = mock(org.locationtech.jts.geom.MultiPolygon.class);
    var maskFileMock = mock(File.class);
    var detectionResponseMock = mock(DetectionResponseV2.class);
    var machineDetectedTileMock = mock(MachineDetectedTile.class);
    var providedFeatureMockDomain = mock(app.bpartners.geojobs.repository.model.Feature.class);
    var roofDelimitationMockDomain = mock(app.bpartners.geojobs.repository.model.Feature.class);
    var roofFeatureGeometryMock =
        mock(app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.class);
    var tileDetectionTask =
        TileDetectionTask.builder()
            .parcelId(parcelId)
            .zoneDetectionJobId(zoneDetectionJobId)
            .detectableObjectConfigurations(detectableObjectConfigurations)
            .tile(tileMock)
            .jobId(parcelJobId)
            .build();
    var multiPolygonFromTileMock = mock(org.locationtech.jts.geom.MultiPolygon.class);
    when(tileMock.getCoordinates()).thenReturn(new TileCoordinates().x(0).y(0).z(20));
    when(roofDelimitationMockDomain.getGeometry()).thenReturn(mock());
    when(roofMultiPolygonMock.union(any())).thenReturn(roofMultiPolygonMock);
    when(geometryConverterMock.apply(featureMultiPolygonMock.getCoordinates()))
        .thenReturn(roofMultiPolygonMock);
    when(featureGeometryMock.getMultiPolygon()).thenReturn(featureMultiPolygonMock);
    when(featureGeometryMock.getActualInstance()).thenReturn(featureMultiPolygonMock);
    when(featureMock.getGeometry()).thenReturn(featureGeometryMock);
    when(detectionMock.hasToitureModelName()).thenReturn(true);
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(List.of(featureMock));
    when(roofFeatureGeometryMock.getActualInstanceStringValue())
        .thenReturn("roofGeometryActualInstanceStringValue");
    when(roofDelimitationMockDomain.getGeometry()).thenReturn(roofFeatureGeometryMock);
    when(roofMultiPolygonMock.contains(any())).thenReturn(false);
    when(roofMultiPolygonMock.intersection(any())).thenReturn(null);
    when(multiPolygonFromTileMock.contains(any())).thenReturn(false);
    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(
            List.of(
                new FeatureWithDelimitation(
                    providedFeatureMockDomain, List.of(roofDelimitationMockDomain))));
    when(detectionRepositoryMock.findByZdjId(zoneDetectionJobId))
        .thenReturn(Optional.of(detectionMock));
    when(geometryConverterMock.centroidFromGeometry(featureMultiPolygonMock))
        .thenReturn(centroidCoordinates);
    when(geometryConverterMock.getMultiPolygonFromTile(eq(0), eq(0), eq(20)))
        .thenReturn(multiPolygonFromTileMock);
    when(machineDetectedTileRepositoryMock.save(any())).thenReturn(machineDetectedTileMock);
    when(objectDetectorMock.apply(any(), any(), any())).thenReturn(detectionResponseMock);
    when(detectionMapperMock.toDetectedTile(any(), any(), any(), any(), any()))
        .thenReturn(new MachineDetectedTile());
    when(maskRetrieverMock.apply(tileMock, roofMultiPolygonMock)).thenReturn(maskFileMock);
    when(geometryConverterMock.readGeometryFromString(eq("roofGeometryActualInstanceStringValue")))
        .thenReturn(roofMultiPolygonMock);
    when(roofCoveringDetectorMock.apply(any(Tile.class), any(File.class)))
        .thenReturn(
            new RoofCoveringDetector.RoofCoveringDetectionResponse(
                new RoofCovering(RoofCoveringType.ROOF_ARDOISE, 1100L),
                new RoofCovering(RoofCoveringType.ROOF_TUILES, 1000L)));

    assertDoesNotThrow(() -> subject.accept(tileDetectionTask));

    verify(geometryConverterMock, times(1)).getMultiPolygonFromTile(eq(0), eq(0), eq(20));
    verify(machineDetectedTileRepositoryMock, never()).save(any());
    verify(objectDetectorMock, never()).apply(any(), any(), any());
    verify(detectionMapperMock, never()).toDetectedTile(any(), any(), any(), any(), any());
  }

  @Test
  void generate_mask_for_toiture_detection_and_save_machine_detected_tile() {
    var detectableObjectConfigurations = List.of(mock(DetectableObjectConfiguration.class));
    var zoneDetectionJobId = randomUUID().toString();
    var parcelId = randomUUID().toString();
    var parcelJobId = randomUUID().toString();
    var tileMock = mock(Tile.class);
    var detectionMock = mock(Detection.class);
    var featureMock = mock(Feature.class);
    var featureGeometryMock = mock(FeatureGeometry.class);
    var featureMultiPolygonMock = mock(MultiPolygon.class);
    var roofMultiPolygonMock = mock(org.locationtech.jts.geom.MultiPolygon.class);
    var maskFileMock = mock(File.class);
    var detectionResponseMock = mock(DetectionResponseV2.class);
    var machineDetectedTileMock = mock(MachineDetectedTile.class);
    var providedFeatureMockDomain = mock(app.bpartners.geojobs.repository.model.Feature.class);
    var roofDelimitationMockDomain = mock(app.bpartners.geojobs.repository.model.Feature.class);
    var roofFeatureGeometryMock =
        mock(app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.class);
    var tileDetectionTask =
        TileDetectionTask.builder()
            .parcelId(parcelId)
            .zoneDetectionJobId(zoneDetectionJobId)
            .detectableObjectConfigurations(detectableObjectConfigurations)
            .tile(tileMock)
            .jobId(parcelJobId)
            .build();
    var tileBuilderMock = mock(Tile.TileBuilder.class);
    when(tileBuilderMock.detectionE2Id(any())).thenReturn(tileBuilderMock);
    when(tileBuilderMock.build()).thenReturn(tileMock);
    when(tileMock.toBuilder()).thenReturn(tileBuilderMock);
    when(tileMock.getCoordinates()).thenReturn(new TileCoordinates().x(0).y(0).z(20));
    when(roofDelimitationMockDomain.getGeometry()).thenReturn(mock());
    when(geometryConverterMock.apply(featureMultiPolygonMock.getCoordinates()))
        .thenReturn(roofMultiPolygonMock);
    when(featureGeometryMock.getMultiPolygon()).thenReturn(featureMultiPolygonMock);
    when(featureGeometryMock.getActualInstance()).thenReturn(featureMultiPolygonMock);
    when(featureMock.getGeometry()).thenReturn(featureGeometryMock);
    when(detectionMock.hasToitureModelName()).thenReturn(true);
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(List.of(featureMock));
    when(roofFeatureGeometryMock.getActualInstanceStringValue())
        .thenReturn("roofGeometryActualInstanceStringValue");
    var multiPolygonFromTileMock = mock(org.locationtech.jts.geom.MultiPolygon.class);
    when(roofDelimitationMockDomain.getGeometry()).thenReturn(roofFeatureGeometryMock);
    when(roofMultiPolygonMock.contains(eq(multiPolygonFromTileMock))).thenReturn(true);
    when(multiPolygonFromTileMock.intersection(roofMultiPolygonMock))
        .thenReturn(roofMultiPolygonMock);
    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(
            List.of(
                new FeatureWithDelimitation(
                    providedFeatureMockDomain, List.of(roofDelimitationMockDomain))));
    when(detectionRepositoryMock.findByZdjId(zoneDetectionJobId))
        .thenReturn(Optional.of(detectionMock));
    when(geometryConverterMock.getMultiPolygonFromTile(0, 0, 20))
        .thenReturn(multiPolygonFromTileMock);
    when(geometryConverterMock.readGeometryFromString(eq("roofGeometryActualInstanceStringValue")))
        .thenReturn(roofMultiPolygonMock);
    when(roofMultiPolygonMock.intersection(any())).thenReturn(roofMultiPolygonMock);
    when(machineDetectedTileRepositoryMock.save(any())).thenReturn(machineDetectedTileMock);
    when(objectDetectorMock.apply(any(), any(), any())).thenReturn(detectionResponseMock);
    when(detectionMapperMock.toDetectedTile(any(), any(), any(), any(), any()))
        .thenReturn(new MachineDetectedTile());
    when(maskRetrieverMock.apply(tileMock, roofMultiPolygonMock)).thenReturn(maskFileMock);
    when(roofCoveringDetectorMock.apply(any(Tile.class), any(File.class)))
        .thenReturn(
            new RoofCoveringDetector.RoofCoveringDetectionResponse(
                new RoofCovering(RoofCoveringType.ROOF_ARDOISE, 1100L),
                new RoofCovering(RoofCoveringType.ROOF_TUILES, 1000L)));

    assertDoesNotThrow(() -> subject.accept(tileDetectionTask));

    var detectedTileCaptor = ArgumentCaptor.forClass(MachineDetectedTile.class);
    verify(geometryConverterMock, times(1)).getMultiPolygonFromTile(eq(0), eq(0), eq(20));
    verify(machineDetectedTileRepositoryMock, times(1)).save(detectedTileCaptor.capture());
    verify(objectDetectorMock)
        .apply(eq(tileDetectionTask), eq(maskFileMock), eq(detectableObjectConfigurations));
    verify(detectionMapperMock)
        .toDetectedTile(
            eq(detectionResponseMock),
            eq(tileMock),
            eq(parcelId),
            eq(zoneDetectionJobId),
            eq(parcelJobId));
  }

  @Test
  void mask_null_when_detection_not_toiture() {
    var detectableObjectConfigurations = List.of(mock(DetectableObjectConfiguration.class));
    var zoneDetectionJobId = randomUUID().toString();
    var parcelId = randomUUID().toString();
    var parcelJobId = randomUUID().toString();
    var tileMock = mock(Tile.class);
    var detectionMock = mock(Detection.class);
    var featureMock = mock(Feature.class);
    var featureGeometryMock = mock(FeatureGeometry.class);
    var featureMultiPolygonMock = mock(MultiPolygon.class);
    var centroidCoordinates = List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(1));
    var detectionResponseMock = mock(DetectionResponseV2.class);
    var machineDetectedTileMock = mock(MachineDetectedTile.class);
    var providedFeatureMockDomain = mock(app.bpartners.geojobs.repository.model.Feature.class);
    var roofDelimitationMockDomain = mock(app.bpartners.geojobs.repository.model.Feature.class);
    var tileDetectionTask =
        TileDetectionTask.builder()
            .parcelId(parcelId)
            .zoneDetectionJobId(zoneDetectionJobId)
            .detectableObjectConfigurations(detectableObjectConfigurations)
            .tile(tileMock)
            .jobId(parcelJobId)
            .build();
    var tileBuilderMock = mock(Tile.TileBuilder.class);
    when(tileBuilderMock.detectionE2Id(any())).thenReturn(tileBuilderMock);
    when(tileBuilderMock.build()).thenReturn(tileMock);
    when(tileMock.toBuilder()).thenReturn(tileBuilderMock);
    when(featureGeometryMock.getMultiPolygon()).thenReturn(featureMultiPolygonMock);
    when(featureGeometryMock.getActualInstance()).thenReturn(featureMultiPolygonMock);
    when(featureMock.getGeometry()).thenReturn(featureGeometryMock);
    when(detectionMock.hasToitureModelName()).thenReturn(false);
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(List.of(featureMock));
    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(
            List.of(
                new FeatureWithDelimitation(
                    providedFeatureMockDomain, List.of(roofDelimitationMockDomain))));
    when(detectionRepositoryMock.findByZdjId(zoneDetectionJobId))
        .thenReturn(Optional.of(detectionMock));
    when(geometryConverterMock.centroidFromGeometry(featureMultiPolygonMock))
        .thenReturn(centroidCoordinates);
    when(machineDetectedTileRepositoryMock.save(any())).thenReturn(machineDetectedTileMock);
    when(objectDetectorMock.apply(any(), any(), any())).thenReturn(detectionResponseMock);
    when(detectionMapperMock.toDetectedTile(any(), any(), any(), any(), any()))
        .thenReturn(new MachineDetectedTile());
    when(roofCoveringDetectorMock.apply(any(Tile.class), eq(null)))
        .thenReturn(
            new RoofCoveringDetector.RoofCoveringDetectionResponse(
                new RoofCovering(RoofCoveringType.ROOF_ARDOISE, 1100L),
                new RoofCovering(RoofCoveringType.ROOF_TUILES, 1000L)));

    assertDoesNotThrow(() -> subject.accept(tileDetectionTask));

    var detectedTileCaptor = ArgumentCaptor.forClass(MachineDetectedTile.class);
    verify(geometryConverterMock, never()).getMultiPolygonFromTile(anyInt(), anyInt(), anyInt());
    verify(machineDetectedTileRepositoryMock, times(1)).save(detectedTileCaptor.capture());
    verify(objectDetectorMock)
        .apply(eq(tileDetectionTask), eq(null), eq(detectableObjectConfigurations));
    verify(detectionMapperMock)
        .toDetectedTile(
            eq(detectionResponseMock),
            eq(tileMock),
            eq(parcelId),
            eq(zoneDetectionJobId),
            eq(parcelJobId));
  }
}
