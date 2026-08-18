package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.repository.model.detection.RoofCoveringType.*;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionRoofPropertiesRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.mockito.ArgumentCaptor;

class DetectionRoofPropertiesRequestedServiceTest {
  DetectionRepository detectionRepositoryMock = mock();
  MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  GeometryConverter geometryConverterMock = mock();
  ObjectMapper objectMapperMock = new ObjectMapper().findAndRegisterModules();
  EntityManager entityManagerMock = mock();
  DetectionPropertiesService featurePropertiesServiceMock = mock();
  EventProducer eventProducerMock = mock();
  GeoJsonConversionJobService geoJsonConversionJobServiceMock = mock();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  DetectionRoofPropertiesRequestedService subject =
      new DetectionRoofPropertiesRequestedService(
          detectionRepositoryMock,
          machineDetectedTileRepositoryMock,
          geometryConverterMock,
          objectMapperMock,
          featurePropertiesServiceMock,
          eventProducerMock,
          geoJsonConversionJobServiceMock,
          zoneDetectionJobServiceMock);

  @Test
  void compute_detection_roof_properties() {
    var detectionIdentifier = randomUUID().toString();
    var zoneDetectionJobIdentifier = randomUUID().toString();
    var providedZoneFeature = mock(Feature.class);
    var firstRoofLatLonMultiPolygonMock = mock(MultiPolygon.class);
    var secondRoofLatLonMultiPolygonMock = mock(MultiPolygon.class);
    var multiPolygonFromTileMock = mock(MultiPolygon.class);
    var secondMultiPolygonFromTileMock = mock(MultiPolygon.class);

    var detectionMock =
        Detection.builder()
            .id(detectionIdentifier)
            .zdjId(zoneDetectionJobIdentifier)
            .detectableObjectModelList(List.of(new DetectableObjectModel().modelName(TOITURE)))
            .featureWithDelimitations(
                List.of(
                    new FeatureWithDelimitation(
                        providedZoneFeature,
                        List.of(featureDelimiterOne(Map.of()), featureDelimiterTwo(Map.of())))))
            .build();
    when(detectionRepositoryMock.findById(detectionIdentifier))
        .thenReturn(Optional.of(detectionMock));
    when(machineDetectedTileRepositoryMock.findAllByZdjJobId(zoneDetectionJobIdentifier))
        .thenReturn(
            List.of(
                MachineDetectedTile.builder()
                    .tile(Tile.builder().coordinates(new TileCoordinates().x(0).y(0).z(0)).build())
                    .primaryRoofCoveringArea(100L)
                    .primaryRoofCoveringType(ROOF_ARDOISE)
                    .secondaryRoofCoveringArea(200L)
                    .secondaryRoofCoveringType(ROOF_TUILES)
                    .build(),
                MachineDetectedTile.builder()
                    .tile(Tile.builder().coordinates(new TileCoordinates().x(0).y(0).z(0)).build())
                    .primaryRoofCoveringArea(50L)
                    .primaryRoofCoveringType(ROOF_TUILES)
                    .secondaryRoofCoveringArea(300L)
                    .secondaryRoofCoveringType(ROOF_BETON_BRUT)
                    .build(),
                MachineDetectedTile.builder()
                    .tile(Tile.builder().coordinates(new TileCoordinates().x(0).y(0).z(0)).build())
                    .primaryRoofCoveringArea(80L)
                    .primaryRoofCoveringType(ROOF_BETON_BRUT)
                    .secondaryRoofCoveringArea(250L)
                    .secondaryRoofCoveringType(ROOF_ARDOISE)
                    .build(),
                MachineDetectedTile.builder()
                    .tile(Tile.builder().coordinates(new TileCoordinates().x(1).y(1).z(1)).build())
                    .primaryRoofCoveringArea(80L)
                    .primaryRoofCoveringType(ROOF_GRAVIER)
                    .secondaryRoofCoveringArea(250L)
                    .secondaryRoofCoveringType(ROOF_ASPHALTE_BITUME)
                    .build()));
    when(multiPolygonFromTileMock.intersects(firstRoofLatLonMultiPolygonMock)).thenReturn(true);
    when(secondMultiPolygonFromTileMock.intersects(secondRoofLatLonMultiPolygonMock))
        .thenReturn(true);
    when(geometryConverterMock.getMultiPolygonFromTile(0, 0, 0))
        .thenReturn(multiPolygonFromTileMock);
    when(geometryConverterMock.getMultiPolygonFromTile(1, 1, 1))
        .thenReturn(secondMultiPolygonFromTileMock);
    when(geometryConverterMock.apply(any()))
        .thenReturn(firstRoofLatLonMultiPolygonMock)
        .thenReturn(secondRoofLatLonMultiPolygonMock);
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    doNothing().when(entityManagerMock).clear();

    assertDoesNotThrow(
        () -> subject.accept(new DetectionRoofPropertiesRequested(detectionIdentifier)));

    var detectionCaptor = ArgumentCaptor.forClass(Detection.class);
    verify(detectionRepositoryMock).save(detectionCaptor.capture());
    assertEquals(
        detectionMock.toBuilder()
            .featureWithDelimitations(
                List.of(
                    new FeatureWithDelimitation(
                        providedZoneFeature,
                        List.of(
                            featureDelimiterOne(
                                Map.of(
                                    "covering",
                                    "{\"primary\":\"ROOF_BETON_BRUT\",\"secondary\":\"ROOF_ARDOISE\"}")),
                            featureDelimiterTwo(
                                Map.of(
                                    "covering",
                                    "{\"primary\":\"ROOF_ASPHALTE_BITUME\",\"secondary\":\"ROOF_GRAVIER\"}"))))))
            .build(),
        detectionCaptor.getValue());
  }

  @Test
  void chains_result_properties_vgg_and_geojson_conversion_after_covering() {
    var detectionIdentifier = randomUUID().toString();
    var zoneDetectionJobIdentifier = randomUUID().toString();
    var providedFeature = featureDelimiterOne(Map.of());
    var detection =
        Detection.builder()
            .id(detectionIdentifier)
            .zdjId(zoneDetectionJobIdentifier)
            .needsImageOutput(true)
            .detectableObjectModelList(List.of(new DetectableObjectModel().modelName(TOITURE)))
            .featureWithDelimitations(List.of())
            .providedGeoJsonZone(List.of(providedFeature))
            .build();
    var zoneDetectionJobMock = mock(ZoneDetectionJob.class);
    when(detectionRepositoryMock.findById(detectionIdentifier)).thenReturn(Optional.of(detection));
    when(machineDetectedTileRepositoryMock.findAllByZdjJobId(zoneDetectionJobIdentifier))
        .thenReturn(List.of());
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(featurePropertiesServiceMock.apply(any(), any(), any())).thenReturn(detection);
    when(zoneDetectionJobServiceMock.findById(zoneDetectionJobIdentifier))
        .thenReturn(zoneDetectionJobMock);

    assertDoesNotThrow(
        () -> subject.accept(new DetectionRoofPropertiesRequested(detectionIdentifier)));

    verify(featurePropertiesServiceMock, times(1)).apply(any(), any(), any());
    var vggCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock).accept(vggCaptor.capture());
    var vggEvent = (FeatureVggRequested) vggCaptor.getValue().getFirst();
    assertEquals(detectionIdentifier, vggEvent.getDetectionIdentifier());
    verify(geoJsonConversionJobServiceMock, times(1))
        .getOrComputeGeoJsonConversionJob(zoneDetectionJobMock);
  }

  @Test
  void chains_geojson_conversion_without_vgg_when_image_output_not_needed() {
    var detectionIdentifier = randomUUID().toString();
    var zoneDetectionJobIdentifier = randomUUID().toString();
    var providedFeature = featureDelimiterOne(Map.of());
    var detection =
        Detection.builder()
            .id(detectionIdentifier)
            .zdjId(zoneDetectionJobIdentifier)
            .needsImageOutput(false)
            .detectableObjectModelList(List.of(new DetectableObjectModel().modelName(TOITURE)))
            .featureWithDelimitations(List.of())
            .providedGeoJsonZone(List.of(providedFeature))
            .build();
    var zoneDetectionJobMock = mock(ZoneDetectionJob.class);
    when(detectionRepositoryMock.findById(detectionIdentifier)).thenReturn(Optional.of(detection));
    when(machineDetectedTileRepositoryMock.findAllByZdjJobId(zoneDetectionJobIdentifier))
        .thenReturn(List.of());
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(featurePropertiesServiceMock.apply(any(), any(), any())).thenReturn(detection);
    when(zoneDetectionJobServiceMock.findById(zoneDetectionJobIdentifier))
        .thenReturn(zoneDetectionJobMock);

    assertDoesNotThrow(
        () -> subject.accept(new DetectionRoofPropertiesRequested(detectionIdentifier)));

    verify(featurePropertiesServiceMock, times(1)).apply(any(), any(), any());
    verify(eventProducerMock, never()).accept(any());
    verify(geoJsonConversionJobServiceMock, times(1))
        .getOrComputeGeoJsonConversionJob(zoneDetectionJobMock);
  }

  private static Feature featureDelimiterTwo(Map<String, Object> properties) {
    return Feature.builder()
        .properties(new HashMap<>(properties))
        .geometry(
            new Feature.FeatureGeometry(
                MULTI_POLYGON,
                "{\"coordinates\":[[[[10,15],[15,15],[15,10],[10,10]]]],\"type\":\"MultiPolygon\"}"))
        .build();
  }

  private static Feature featureDelimiterOne(Map<String, Object> properties) {
    return Feature.builder()
        .properties(new HashMap<>(properties))
        .geometry(
            new Feature.FeatureGeometry(
                POLYGON, "{\"coordinates\":[[[0,5],[5,5],[5,0],[0,0]]],\"type\":\"Polygon\"}"))
        .build();
  }
}
