package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.repository.model.feature.FeatureDelimitationComputing;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class DetectionTest {
  @Test
  void compute_delimitations_from_attribute() {
    List<FeatureWithDelimitation> delimitationsFromAttributeMock =
        List.of(new FeatureWithDelimitation(mock(), mock()));
    Detection subjectWithNullComputingList =
        Detection.builder()
            .featureWithDelimitations(delimitationsFromAttributeMock)
            .featureDelimitationComputingList(null)
            .build();
    Detection subjectWithEmptyComputingList =
        Detection.builder()
            .featureWithDelimitations(delimitationsFromAttributeMock)
            .featureDelimitationComputingList(List.of())
            .build();

    assertEquals(
        delimitationsFromAttributeMock, subjectWithNullComputingList.getFeatureWithDelimitations());
    assertEquals(
        delimitationsFromAttributeMock,
        subjectWithEmptyComputingList.getFeatureWithDelimitations());
  }

  @Test
  void compute_delimitations_from_persisted_delimitation_computing_list() {
    var featureId = randomUUID().toString();
    var domainFeature = mock(Feature.class);
    when(domainFeature.getId()).thenReturn(featureId);
    when(domainFeature.getGeometry()).thenReturn(somePolygon());
    when(domainFeature.getProperties()).thenReturn(new HashMap<>(Map.of("feature_id", featureId)));
    List<FeatureWithDelimitation> delimitationsFromAttributeMock =
        List.of(new FeatureWithDelimitation(domainFeature, List.of()));

    List<Feature> computedDelimitations = List.of(mock(Feature.class), mock(Feature.class));

    FeatureWithDelimitation computedFeature =
        new FeatureWithDelimitation(domainFeature, computedDelimitations);

    FeatureDelimitationComputing computing = mock(FeatureDelimitationComputing.class);
    when(computing.getFeaturePropertiesIdentifier()).thenReturn(featureId);
    when(computing.getCreationDatetime()).thenReturn(now());
    when(computing.getFeatureWithDelimitation()).thenReturn(computedFeature);

    Detection subject =
        Detection.builder()
            .featureWithDelimitations(delimitationsFromAttributeMock)
            .featureDelimitationComputingList(List.of(computing))
            .build();

    List<FeatureWithDelimitation> actual = subject.getFeatureWithDelimitations();

    assertEquals(1, actual.size());
    assertEquals(computedFeature, actual.getFirst());
    assertNotEquals(delimitationsFromAttributeMock, actual);
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
