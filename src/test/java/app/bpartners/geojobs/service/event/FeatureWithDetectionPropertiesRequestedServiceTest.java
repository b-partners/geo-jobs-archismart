package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.ROOF;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE_CLAIR;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.FeatureWithDetectionPropertiesRequested;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.FeatureRoofResultPropertiesComputer;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.geojson.GeometryCorrector;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

class FeatureWithDetectionPropertiesRequestedServiceTest {
  EntityManager entityManagerMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  FeatureRoofResultPropertiesComputer featureRoofResultPropertiesComputerMock = mock();
  GeometryConverter geometryConverterMock = mock();
  MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  GeometryCorrector geometryCorrectorMock = mock();
  FeatureWithDetectionPropertiesRequestedService subject =
      new FeatureWithDetectionPropertiesRequestedService(
          entityManagerMock,
          detectionRepositoryMock,
          featureRoofResultPropertiesComputerMock,
          geometryConverterMock,
          machineDetectedTileRepositoryMock,
          eventProducerMock,
          geometryCorrectorMock);

  @Test
  void compute_feature_with_detection_properties_requested_and_produces_vgg() {
    var detectionIdentifier = randomUUID().toString();
    var zoneDetectionJobIdentifier = randomUUID().toString();
    var featureId = randomUUID().toString();
    var featureMock = mock(Feature.class);
    var detectionMock = mock(Detection.class);
    var tile = someTile();
    var providedGeometryMultiPolygon = new MultiPolygon();
    var providedFeatureGeometry = new FeatureGeometry(providedGeometryMultiPolygon);
    var detectedObjectGeometryMultiPolygon = new MultiPolygon();
    var detectedObjectFeatureGeometry = new FeatureGeometry(detectedObjectGeometryMultiPolygon);
    var detectedObjectIntersectionWithProvidedFeature = mock(Polygon.class);
    var latLonRoofGeometryMock = mock(org.locationtech.jts.geom.MultiPolygon.class);
    var machineDetectedTileMock = mock(MachineDetectedTile.class);
    var detectedObjectMock = mock(DetectedObject.class);
    var detectedObjectFeatureMock = mock(Feature.class);
    var featureProperties = new HashMap<String, Object>(Map.of("id", featureId));
    var newPropertiesMock = new HashMap<String, Object>(Map.of("roof_area_in_m2", 100.0));
    var expectedFeatureProperties =
        new HashMap<String, Object>(Map.of("id", featureId, "roof_area_in_m2", 100.0));

    when(latLonRoofGeometryMock.intersection(any()))
        .thenReturn(detectedObjectIntersectionWithProvidedFeature);

    when(detectionMock.hasToitureModelName()).thenReturn(true);
    when(detectionMock.getZdjId()).thenReturn(zoneDetectionJobIdentifier);
    when(detectionMock.getGeoJsonDelimitationType()).thenReturn(ROOF);
    when(detectionMock.getDelimitationOf(featureMock)).thenReturn(List.of(featureMock));

    when(featureMock.getProperties()).thenReturn(featureProperties);
    when(featureMock.getGeometry()).thenReturn(providedFeatureGeometry);

    when(detectedObjectFeatureMock.getGeometry()).thenReturn(detectedObjectFeatureGeometry);

    when(detectedObjectMock.getDetectableObjectType()).thenReturn(MOISISSURE_CLAIR);
    when(detectedObjectMock.getFeature()).thenReturn(detectedObjectFeatureMock);

    when(machineDetectedTileMock.getTile()).thenReturn(tile);
    when(machineDetectedTileMock.getDetectedObjects()).thenReturn(List.of(detectedObjectMock));

    when(detectionRepositoryMock.findById(detectionIdentifier))
        .thenReturn(Optional.of(detectionMock));
    when(geometryConverterMock.apply(any())).thenReturn(latLonRoofGeometryMock);
    when(geometryCorrectorMock.apply(any())).thenReturn(latLonRoofGeometryMock);
    when(machineDetectedTileRepositoryMock.findAllByZdjJobId(zoneDetectionJobIdentifier))
        .thenReturn(List.of(machineDetectedTileMock));
    when(featureRoofResultPropertiesComputerMock.apply(
            featureMock,
            latLonRoofGeometryMock,
            latLonRoofGeometryMock,
            List.of(
                new PolygonObjectType(
                    detectedObjectIntersectionWithProvidedFeature, MOISISSURE_CLAIR))))
        .thenReturn(newPropertiesMock);

    assertDoesNotThrow(
        () ->
            subject.accept(
                new FeatureWithDetectionPropertiesRequested(detectionIdentifier, featureMock)));

    verify(detectionMock).addFeatureDelimitations(any(), any());

    verify(detectionRepositoryMock).save(detectionMock);
  }

  private Tile someTile() {
    return Tile.builder().coordinates(new TileCoordinates().x(0).y(1).z(20)).build();
  }

  @Test
  void throw_no_such_element_exception_when_detection_not_found() {
    var detectionIdentifier = randomUUID().toString();
    when(detectionRepositoryMock.findById(detectionIdentifier)).thenReturn(Optional.empty());

    var actual =
        assertThrows(
            NoSuchElementException.class,
            () ->
                subject.accept(
                    new FeatureWithDetectionPropertiesRequested(detectionIdentifier, null)));

    assertEquals("No value present", actual.getMessage());
  }

  @Test
  void produces_feature_vgg_computing_event_for_non_toiture_model() {
    var detectionIdentifier = randomUUID().toString();
    var featureMock = mock(Feature.class);
    var detectionMock = mock(Detection.class);

    when(detectionMock.hasToitureModelName()).thenReturn(false);
    when(detectionRepositoryMock.findById(detectionIdentifier))
        .thenReturn(Optional.of(detectionMock));

    assertDoesNotThrow(
        () ->
            subject.accept(
                new FeatureWithDetectionPropertiesRequested(detectionIdentifier, featureMock)));

    verify(featureRoofResultPropertiesComputerMock, never()).apply(any(), any(), any(), any());
    verify(detectionRepositoryMock, never()).save(any());
    verify(machineDetectedTileRepositoryMock, never()).save(any());
    verify(eventProducerMock, only()).accept(any());
    verify(geometryConverterMock, never()).apply(any());
  }
}
