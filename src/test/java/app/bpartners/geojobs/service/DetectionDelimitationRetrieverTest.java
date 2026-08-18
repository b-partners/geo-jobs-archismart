package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.USER_DEFINED_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.ZONE;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.BuildingFootPrintSourceUnavailableException;
import app.bpartners.geojobs.model.geometry.RoofDetails;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.ign.IgnCadastreFeatureFetcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;

class DetectionDelimitationRetrieverTest {
  GeometryConverter geometryConverterMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  ObjectMapper objectMapperMock = mock();
  IgnCadastreFeatureFetcher ignCadastreFeatureFetcher = mock();
  BuildingFinder buildingFinderMock = mock();
  DetectionDelimitationRetriever subject =
      new DetectionDelimitationRetriever(
          geometryConverterMock,
          detectionRepositoryMock,
          objectMapperMock,
          ignCadastreFeatureFetcher,
          buildingFinderMock);

  @SneakyThrows
  @Test
  void feature_with_delimitation_from_provided_geoJson_when_geoJsonDelimitation_type_is_roof()
      throws JsonProcessingException {
    var providedGeoJson = multiPolygonFeature();
    var detection = detection(List.of(providedGeoJson), ROOF);

    when(detectionRepositoryMock.save(any())).thenReturn(detection);

    subject.apply(detection);

    var featureWithDelimitationAsString = featureWithGeometryAsString(providedGeoJson);
    var expectedDetectionToBeSaved =
        detection.toBuilder()
            .pointDelimitation(
                new HashMap<>(
                    Map.of(
                        new ObjectMapper().writeValueAsString(featureWithDelimitationAsString),
                        featureWithDelimitationAsString)))
            .featureWithDelimitations(
                List.of(
                    new FeatureWithDelimitation(
                        providedGeoJson, List.of(featureWithDelimitationAsString))))
            .build();

    verify(detectionRepositoryMock, times(1)).save(expectedDetectionToBeSaved);
  }

  @Test
  void building_api_should_be_called_when_geoJsonDelimitation_type_is_zone() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), ZONE);

    when(detectionRepositoryMock.save(any())).thenReturn(detection);
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.apply(detection);

    verify(buildingFinderMock, times(1)).retrieveRoofPolygonsFrom(any());
  }

  @Test
  void building_api_should_be_called_when_geoJsonDelimitation_type_is_null() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), null);

    when(detectionRepositoryMock.save(any())).thenReturn(detection);
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.apply(detection);

    verify(buildingFinderMock, times(1)).retrieveRoofPolygonsFrom(any());
  }

  @Test
  void polygon_is_mapped_to_multipolygon_when_delimitation_type_is_roof() {
    var polygonFeature = polygonFeature();
    var detection = detection(List.of(polygonFeature), ROOF);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    subject.apply(detection);

    verify(detectionRepositoryMock)
        .save(
            argThat(
                savedDetection -> {
                  var fwd = savedDetection.getFeatureWithDelimitations().getFirst();
                  var delimitations = fwd.delimitations();
                  if (delimitations.size() != 1) {
                    return false;
                  }

                  var delimitation = delimitations.getFirst();
                  return MULTI_POLYGON.equals(delimitation.getGeometry().getGeometryType());
                }));
  }

  @Test
  void provided_geometry_is_used_as_delimitation_when_delimitation_type_is_user_defined() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.apply(detection);

    var delimitation = savedDelimitation();
    assertEquals(MULTI_POLYGON, delimitation.getGeometry().getGeometryType());
    var delimitationCoordinates =
        toRestFeature(delimitation).getGeometry().getMultiPolygon().getCoordinates();
    assertEquals(1, delimitationCoordinates.size());
    assertEquals(1, delimitationCoordinates.getFirst().size());
    assertEquals(PROVIDED_POLYGON_RING, asDoubles(delimitationCoordinates.getFirst().getFirst()));
  }

  @Test
  void building_api_is_called_with_provided_ring_when_delimitation_type_is_user_defined() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.apply(detection);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<List<BigDecimal>>> ringCaptor = ArgumentCaptor.forClass(List.class);
    verify(buildingFinderMock, times(1)).retrieveRoofPolygonsFrom(ringCaptor.capture());
    assertEquals(PROVIDED_POLYGON_RING, asDoubles(ringCaptor.getValue()));
  }

  @SneakyThrows
  @Test
  void addresses_property_is_computed_from_rnb_when_delimitation_type_is_user_defined() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);
    var addresses = List.of("1 rue de la Paix Paris 75002");
    var serializedAddresses = "[\"1 rue de la Paix Paris 75002\"]";

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenReturn(List.of(new RoofDetails(null, addresses)));
    when(objectMapperMock.writeValueAsString(addresses)).thenReturn(serializedAddresses);

    subject.apply(detection);

    assertEquals(serializedAddresses, savedDelimitation().getProperties().get("addresses"));
  }

  @Test
  void addresses_property_is_not_set_when_no_building_is_found_inside_provided_zone() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.apply(detection);

    assertFalse(savedDelimitation().getProperties().containsKey("addresses"));
  }

  @Test
  void delimitation_is_still_computed_when_building_api_is_unavailable() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenThrow(new BuildingFootPrintSourceUnavailableException("RNB is unavailable"));

    assertDoesNotThrow(() -> subject.apply(detection));

    var delimitation = savedDelimitation();
    assertEquals(List.of(), delimitation.getProperties().get("addresses"));
    assertEquals(MULTI_POLYGON, delimitation.getGeometry().getGeometryType());
  }

  @Test
  void delimitation_is_still_computed_when_provided_zone_is_larger_than_supported_radius() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenThrow(
            new UnsupportedOperationException(
                "Provided multiPolygon zone is larger than supported retrieving roof polygons"
                    + " radius 1000, actual is 2000"));

    assertDoesNotThrow(() -> subject.apply(detection));

    var delimitation = savedDelimitation();
    assertEquals(List.of(), delimitation.getProperties().get("addresses"));
    assertEquals(MULTI_POLYGON, delimitation.getGeometry().getGeometryType());
  }

  @Test
  void delimitation_is_still_computed_when_building_api_is_unavailable_for_roof() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), ROOF);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenThrow(new BuildingFootPrintSourceUnavailableException("RNB is unavailable"));

    assertDoesNotThrow(() -> subject.apply(detection));

    var delimitation = savedDelimitation();
    assertEquals(List.of(), delimitation.getProperties().get("addresses"));
    assertEquals(MULTI_POLYGON, delimitation.getGeometry().getGeometryType());
  }

  @SneakyThrows
  @Test
  void zoom_and_addresses_are_kept_when_provided_polygon_feature_has_no_properties() {
    var providedGeoJson = polygonFeature().toBuilder().properties(null).build();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);
    var addresses = List.of("1 rue de la Paix Paris 75002");
    var serializedAddresses = "[\"1 rue de la Paix Paris 75002\"]";

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenReturn(List.of(new RoofDetails(null, addresses)));
    when(objectMapperMock.writeValueAsString(addresses)).thenReturn(serializedAddresses);

    subject.apply(detection);

    var delimitation = savedDelimitation();
    assertEquals(serializedAddresses, delimitation.getProperties().get("addresses"));
    assertEquals(20, delimitation.getProperties().get("zoom"));
    assertEquals(20, delimitation.getZoom());
  }

  @SneakyThrows
  @Test
  void zoom_and_addresses_are_kept_when_provided_multipolygon_feature_has_no_properties() {
    var providedGeoJson = multiPolygonFeature().toBuilder().properties(null).build();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);
    var addresses = List.of("1 rue de la Paix Paris 75002");
    var serializedAddresses = "[\"1 rue de la Paix Paris 75002\"]";

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenReturn(List.of(new RoofDetails(null, addresses)));
    when(objectMapperMock.writeValueAsString(addresses)).thenReturn(serializedAddresses);

    subject.apply(detection);

    var delimitation = savedDelimitation();
    assertEquals(serializedAddresses, delimitation.getProperties().get("addresses"));
    assertEquals(20, delimitation.getProperties().get("zoom"));
    assertEquals(20, delimitation.getZoom());
  }

  @Test
  void default_zoom_is_applied_when_provided_feature_has_neither_properties_nor_zoom() {
    var providedGeoJson = polygonFeature().toBuilder().properties(null).zoom(null).build();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.apply(detection);

    var delimitation = savedDelimitation();
    assertEquals(HOUSES_0.getZoomLevel(), delimitation.getProperties().get("zoom"));
    assertEquals(HOUSES_0.getZoomLevel(), delimitation.getZoom());
  }

  private Feature savedDelimitation() {
    var detectionCaptor = ArgumentCaptor.forClass(Detection.class);
    verify(detectionRepositoryMock, times(1)).save(detectionCaptor.capture());

    var featureWithDelimitations = detectionCaptor.getValue().getFeatureWithDelimitations();
    assertEquals(1, featureWithDelimitations.size());
    var delimitations = featureWithDelimitations.getFirst().delimitations();
    assertEquals(1, delimitations.size());
    return delimitations.getFirst();
  }

  private static List<List<Double>> asDoubles(List<List<BigDecimal>> ring) {
    return ring.stream()
        .map(coordinates -> coordinates.stream().map(BigDecimal::doubleValue).toList())
        .toList();
  }

  private static final List<List<Double>> PROVIDED_POLYGON_RING =
      List.of(List.of(0.0, 0.0), List.of(10.0, 0.0), List.of(5.0, 10.0), List.of(0.0, 0.0));

  private static Feature polygonFeature() {
    return Feature.builder()
        .properties(new HashMap<>())
        .zoom(20)
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(POLYGON)
                .actualInstanceStringValue(
                    """
                    {
                      "type": "Polygon",
                      "coordinates": [
                        [
                          [0.0, 0.0],
                          [10.0, 0.0],
                          [5.0, 10.0],
                          [0.0, 0.0]
                        ]
                      ]
                    }
                    """)
                .build())
        .build();
  }

  private static Feature multiPolygonFeature() {
    return Feature.builder()
        .id(null)
        .zoom(20)
        .properties(new HashMap<>())
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(MULTI_POLYGON)
                .actualInstanceStringValue(
                    """
                    {
                      "type": "MultiPolygon",
                      "coordinates": [
                        [
                          [
                            [0.0, 0.0],
                            [10.0, 0.0],
                            [5.0, 10.0],
                            [0.0, 0.0]
                          ]
                        ]
                      ]
                    }
                    """)
                .build())
        .build();
  }

  private static Detection detection(
      List<Feature> providedGeoJson, DelimitationType delimitationType) {
    return Detection.builder()
        .id("detectionId")
        .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
        .providedGeoJsonZone(providedGeoJson)
        .geoJsonDelimitationType(delimitationType)
        .build();
  }

  private static Feature featureWithGeometryAsString(Feature feature) {
    var restFeature = toRestFeature(feature);
    restFeature.putPropertiesItem("zoom", feature.getZoom());
    return toDomainFeature(restFeature);
  }
}
