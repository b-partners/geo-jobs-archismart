package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.*;
import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.LIDAR_DATA_STATUS_PROPERTY_NAME;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.AVAILABLE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.FeatureRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.model.lidar.planes.Plane3DSlopeInDegrees;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor;
import app.bpartners.geojobs.service.lidar.model.geometry.roof.Building3DProperties;
import app.bpartners.geojobs.service.lidar.model.geometry.roof.BuildingHeightInMeters;
import app.bpartners.geojobs.service.lidar.model.geometry.roof.LidarRoofData;
import app.bpartners.geojobs.service.lidar.model.geometry.roof.RoofPlane3D;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;
import org.mockito.ArgumentCaptor;

class FeatureRoofSlopeAndHeightRequestedServiceTest {

  DetectionRepository detectionRepositoryMock = mock();
  LidarRoofsAnalysisProcessor lidarRoofsAnalysisProcessorMock = mock();
  FeatureMapper featureMapperMock = mock();
  EntityManager entityManagerMock = mock();
  EventProducer eventProducerMock = mock();
  FeatureRoofSlopeAndHeightRequestedService subject =
      new FeatureRoofSlopeAndHeightRequestedService(
          detectionRepositoryMock,
          lidarRoofsAnalysisProcessorMock,
          featureMapperMock,
          eventProducerMock,
          entityManagerMock);

  @BeforeEach
  void setUp() {
    doNothing().when(entityManagerMock).clear();
  }

  @Test
  void save_slope_and_height_ok() {
    var expectedRoofSlope = 42.0;
    var expectedRoofHeight = 3.5;
    var detectionIdentifier = randomUUID().toString();
    var featureIdentifier = randomUUID().toString();

    var domainFeatureProperties =
        new HashMap<String, Object>(Map.of("feature_id", featureIdentifier));
    var delimitationFeatureProperties =
        new HashMap<String, Object>(Map.of("feature_id", featureIdentifier));

    var domainFeature =
        Feature.builder()
            .id(featureIdentifier)
            .properties(domainFeatureProperties)
            .geometry(somePolygon())
            .build();
    var domainFeatureDelimitation =
        Feature.builder().properties(delimitationFeatureProperties).geometry(somePolygon()).build();

    var featureWithDelimitations =
        List.of(
            new FeatureWithDelimitation(
                domainFeatureDelimitation, List.of(domainFeatureDelimitation)));
    var detection =
        Detection.builder()
            .id(detectionIdentifier)
            .featureWithDelimitations(featureWithDelimitations)
            .build();
    when(detectionRepositoryMock.findById(detection.getId())).thenReturn(Optional.of(detection));
    when(featureMapperMock.domainToGeometry(any())).thenReturn(mock(Polygon.class));

    var data = mock(LidarRoofData.class);
    var result = mock(LidarRoofsAnalysisProcessor.RoofsAnalysisResult.class);
    var properties = mock(Building3DProperties.class);

    when(data.status()).thenReturn(AVAILABLE);
    when(properties.getData()).thenReturn(data);
    when(result.getProperties(any())).thenReturn(properties);

    var plane = mock(RoofPlane3D.class);
    var slope = mock(Plane3DSlopeInDegrees.class);
    when(plane.getSlopeInDegrees()).thenReturn(slope);
    when(slope.getValue()).thenReturn(expectedRoofSlope);
    when(properties.getRoofPlanes()).thenReturn(List.of(plane));

    var height = mock(BuildingHeightInMeters.class);
    when(height.getValue()).thenReturn(expectedRoofHeight);
    when(properties.getHeightInMeters()).thenReturn(height);
    when(lidarRoofsAnalysisProcessorMock.from(anySet())).thenReturn(result);

    subject.accept(
        FeatureRoofSlopeAndHeightRequested.builder()
            .detectionIdentifier(detection.getId())
            .feature(toRestFeature(domainFeature))
            .build());

    var firstDelimitation =
        detection.getFeatureWithDelimitations().getFirst().delimitations().getFirst();
    var actualRoofSlope = firstDelimitation.getProperties().get(ROOF_SLOPE_PROPERTY_NAME);
    var actualRoofHeight = firstDelimitation.getProperties().get(ROOF_HEIGHT_PROPERTY_NAME);
    var actualRoofDataStatus =
        firstDelimitation.getProperties().get(LIDAR_DATA_STATUS_PROPERTY_NAME);

    var expectedUpdatedProperties = new LinkedHashMap<String, Object>();
    expectedUpdatedProperties.put("feature_id", featureIdentifier);
    expectedUpdatedProperties.put("roof_slope_in_degrees", expectedRoofSlope);
    expectedUpdatedProperties.put("roof_height_in_meters", expectedRoofHeight);
    expectedUpdatedProperties.put("lidar_data_status", AVAILABLE);

    assertEquals(expectedRoofSlope, actualRoofSlope);
    assertEquals(expectedRoofHeight, actualRoofHeight);
    assertEquals(AVAILABLE, actualRoofDataStatus);

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock).accept(eventCaptor.capture());
    var featureVggRequested = (FeatureVggRequested) eventCaptor.getValue().getFirst();
    assertEquals(
        new FeatureVggRequested(detectionIdentifier, toRestFeature(domainFeature)),
        featureVggRequested);
  }

  @Test
  void already_processed_detection_should_not_be_processed() {
    var detectionIdentifier = randomUUID().toString();
    var featureIdentifier = randomUUID().toString();
    var featureProperties = new HashMap<String, Object>(Map.of("feature_id", featureIdentifier));
    var domainFeature =
        Feature.builder()
            .id(featureIdentifier)
            .properties(featureProperties)
            .geometry(somePolygon())
            .build();
    var restFeature = toRestFeature(domainFeature);
    var detectionAlreadyProcessed =
        Detection.builder()
            .id(randomUUID().toString())
            .featureWithDelimitations(
                List.of(
                    new FeatureWithDelimitation(
                        domainFeature,
                        List.of(
                            Feature.builder()
                                .geometry(somePolygon())
                                .properties(
                                    new HashMap<>(
                                        Map.of(LIDAR_DATA_STATUS_PROPERTY_NAME, AVAILABLE)))
                                .build()))))
            .build();
    when(detectionRepositoryMock.findById(any()))
        .thenReturn(Optional.of(detectionAlreadyProcessed));

    assertDoesNotThrow(
        () ->
            subject.accept(
                new FeatureRoofSlopeAndHeightRequested(detectionIdentifier, restFeature)));

    verify(lidarRoofsAnalysisProcessorMock, never()).from(anySet());
    verify(detectionRepositoryMock, never()).save(any());
    verify(eventProducerMock, never()).accept(any());
  }

  @SneakyThrows
  Feature.FeatureGeometry somePolygon() {
    return new Feature.FeatureGeometry(
        POLYGON,
        objectMapper()
            .writeValueAsString(
                new app.bpartners.geojobs.endpoint.rest.model.Polygon()
                    .type(app.bpartners.geojobs.endpoint.rest.model.Polygon.TypeEnum.POLYGON)
                    .coordinates(
                        List.of(List.of(List.of(new BigDecimal("0.0"), new BigDecimal("0.0")))))));
  }
}
