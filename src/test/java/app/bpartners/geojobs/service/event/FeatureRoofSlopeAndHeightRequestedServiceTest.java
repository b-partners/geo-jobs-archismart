package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static app.bpartners.geojobs.model.lidar.LidarProcessorType.THREE_D_BAG_ROOFER;
import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.*;
import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.LIDAR_DATA_STATUS_PROPERTY_NAME;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.AVAILABLE;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.UNAVAILABLE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.FeatureRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.CityJSONSafeModeProcessor;
import app.bpartners.geojobs.service.CityJSONSafeModeProcessor.SafeModeResult;
import app.bpartners.geojobs.service.threed.exception.ThreedApiException;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class FeatureRoofSlopeAndHeightRequestedServiceTest {

  DetectionRepository detectionRepositoryMock = mock();
  CityJSONSafeModeProcessor cityJSONSafeModeProcessorMock = mock();
  BucketComponent bucketComponentMock = mock();
  EntityManager entityManagerMock = mock();
  EventProducer eventProducerMock = mock();
  FeatureRoofSlopeAndHeightRequestedService subject =
      new FeatureRoofSlopeAndHeightRequestedService(
          detectionRepositoryMock,
          cityJSONSafeModeProcessorMock,
          bucketComponentMock,
          eventProducerMock,
          entityManagerMock);

  @BeforeEach
  void setUp() {
    doNothing().when(entityManagerMock).clear();
  }

  // 10m x 10m shed roof: ground at z=100, roof from z=105 to z=110
  private static final String SHED_ROOF_CITYJSON =
      """
      {
        "type": "CityJSON",
        "version": "2.0",
        "vertices": [
          [0, 0, 100], [10, 0, 100], [10, 10, 100], [0, 10, 100],
          [0, 0, 105], [10, 0, 105], [10, 10, 110], [0, 10, 110]
        ],
        "CityObjects": {
          "building-0": {
            "type": "Building",
            "geometry": [{
              "type": "MultiSurface",
              "lod": "2.2",
              "boundaries": [[[0, 3, 2, 1]], [[4, 5, 6, 7]]],
              "semantics": {
                "surfaces": [{"type": "GroundSurface"}, {"type": "RoofSurface"}],
                "values": [0, 1]
              }
            }]
          }
        }
      }
      """;

  @SneakyThrows
  @Test
  void save_slope_and_height_ok() {
    var expectedRoofSlope = 26.57;
    var expectedRoofHeight = 10.0;
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
    var cityJsonFile = File.createTempFile("cityjson", ".json");
    Files.writeString(cityJsonFile.toPath(), SHED_ROOF_CITYJSON);
    when(cityJSONSafeModeProcessorMock.generate(any()))
        .thenReturn(
            new SafeModeResult(
                List.of(CityJSON.builder().s3FileKey("roof.json").build()), THREE_D_BAG_ROOFER));
    when(bucketComponentMock.download("roof.json")).thenReturn(cityJsonFile);

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

    verify(cityJSONSafeModeProcessorMock, never()).generate(any());
    verify(detectionRepositoryMock, never()).save(any());
    verify(eventProducerMock, never()).accept(any());
  }

  @Test
  void save_unavailable_when_no_lidar_data() {
    var featureIdentifier = randomUUID().toString();
    var domainFeature =
        Feature.builder()
            .id(featureIdentifier)
            .properties(new HashMap<>(Map.of("feature_id", featureIdentifier)))
            .geometry(somePolygon())
            .build();
    var domainFeatureDelimitation =
        Feature.builder()
            .properties(new HashMap<>(Map.of("feature_id", featureIdentifier)))
            .geometry(somePolygon())
            .build();
    var detection =
        Detection.builder()
            .id(randomUUID().toString())
            .featureWithDelimitations(
                List.of(
                    new FeatureWithDelimitation(
                        domainFeatureDelimitation, List.of(domainFeatureDelimitation))))
            .build();
    when(detectionRepositoryMock.findById(detection.getId())).thenReturn(Optional.of(detection));
    when(cityJSONSafeModeProcessorMock.generate(any()))
        .thenThrow(
            new ThreedApiException(
                HttpStatus.OK, "NO_LIDAR_DATA_FOUND", "No lidar data found", "No lidar data"));

    subject.accept(
        FeatureRoofSlopeAndHeightRequested.builder()
            .detectionIdentifier(detection.getId())
            .feature(toRestFeature(domainFeature))
            .build());

    var properties =
        detection
            .getFeatureWithDelimitations()
            .getFirst()
            .delimitations()
            .getFirst()
            .getProperties();
    assertEquals(0.0, properties.get(ROOF_SLOPE_PROPERTY_NAME));
    assertEquals(0.0, properties.get(ROOF_HEIGHT_PROPERTY_NAME));
    assertEquals(UNAVAILABLE, properties.get(LIDAR_DATA_STATUS_PROPERTY_NAME));
    verify(detectionRepositoryMock).save(detection);
  }

  @Test
  void rethrow_when_cityjson_generation_fails() {
    var featureIdentifier = randomUUID().toString();
    var domainFeature =
        Feature.builder()
            .id(featureIdentifier)
            .properties(new HashMap<>(Map.of("feature_id", featureIdentifier)))
            .geometry(somePolygon())
            .build();
    var detection =
        Detection.builder()
            .id(randomUUID().toString())
            .featureWithDelimitations(
                List.of(new FeatureWithDelimitation(domainFeature, List.of(domainFeature))))
            .build();
    when(detectionRepositoryMock.findById(detection.getId())).thenReturn(Optional.of(detection));
    when(cityJSONSafeModeProcessorMock.generate(any())).thenThrow(new RuntimeException("boom"));

    assertThrows(
        RuntimeException.class,
        () ->
            subject.accept(
                FeatureRoofSlopeAndHeightRequested.builder()
                    .detectionIdentifier(detection.getId())
                    .feature(toRestFeature(domainFeature))
                    .build()));

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
