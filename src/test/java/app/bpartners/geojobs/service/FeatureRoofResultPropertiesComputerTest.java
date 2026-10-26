package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.ROOF_SLOPE_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.geometry.MultiPolygonObjectType;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.repository.model.detection.RoofCoveringType;
import app.bpartners.geojobs.service.area.mutation.MutationComputer;
import app.bpartners.geojobs.service.area.mutation.model.AreaPictureHistoryResponse;
import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.area.mutation.model.MutationType;
import app.bpartners.geojobs.service.area.toiture.model.CoveringType;
import app.bpartners.geojobs.service.area.toiture.model.FireRiskLevel;
import app.bpartners.geojobs.service.area.toiture.model.MaintenancePriority;
import app.bpartners.geojobs.service.area.toiture.model.RoofAssessmentResult;
import app.bpartners.geojobs.service.area.toiture.model.VegetationIndex;
import app.bpartners.geojobs.service.area.toiture.service.RoofAssessmentFacade;
import app.bpartners.geojobs.service.area.toiture.service.RoofVegetationContextEvaluator;
import app.bpartners.geojobs.service.event.DetectionRoofPropertiesRequestedService.DetectedRoofCovering;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureRoofResultPropertiesComputerTest {

  @Mock private GeometrySquareMeterArea geometrySquareMeterArea;
  @Mock private PolygonObjectTypeConverter polygonObjectTypeConverter;
  @Mock private RoofAssessmentFacade roofAssessmentFacade;
  @Mock private ObjectMapper objectMapper;
  @Mock private MutationComputer mutationComputer;

  private Geometry geometryUsedForAreaComputing;
  private Geometry roofGeometryUsedForRateComputing;

  @Captor private ArgumentCaptor<RoofVegetationContextEvaluator> evaluatorCaptor;

  @InjectMocks private FeatureRoofResultPropertiesComputer subject;

  private Feature feature;
  private Collection<PolygonObjectType> detectedObjects;

  @BeforeEach
  void setUp() {
    GeometryFactory gf = new GeometryFactory();
    geometryUsedForAreaComputing = createSquare(gf, 100);
    roofGeometryUsedForRateComputing = createSquare(gf, 100);

    feature = new Feature();
    detectedObjects = List.of();
    List<MultiPolygonObjectType> convertedObjects = List.of();
    RoofAssessmentResult stubAssessment =
        new RoofAssessmentResult(
            VegetationIndex.FAIBLE, FireRiskLevel.MODERE, MaintenancePriority.PRIORITAIRE);

    lenient().when(geometrySquareMeterArea.apply(any())).thenReturn(150.0);
    lenient()
        .when(polygonObjectTypeConverter.convertFrom(anyCollection()))
        .thenReturn(convertedObjects);
    lenient().when(roofAssessmentFacade.computeAssessment(any())).thenReturn(stubAssessment);
  }

  @Test
  void should_compute_properties_with_full_valid_data() throws Exception {
    Map<String, Object> inputProps = new HashMap<>();
    inputProps.put("existing_key", "existing_value");
    inputProps.put(ROOF_SLOPE_PROPERTY_NAME, 22.5);
    inputProps.put("addresses", "[\"123 Main St\"]");
    inputProps.put(
        "covering", "{\"primary\":\"ROOF_ASPHALTE_BITUME\",\"secondary\":\"ROOF_TUILES\"}");
    feature.setProperties(inputProps);

    List<String> expectedAddresses = List.of("123 Main St");
    DetectedRoofCovering expectedCovering =
        new DetectedRoofCovering(
            RoofCoveringType.ROOF_ASPHALTE_BITUME, RoofCoveringType.ROOF_TUILES);

    when(objectMapper.readValue(eq("[\"123 Main St\"]"), any(TypeReference.class)))
        .thenReturn(expectedAddresses);
    when(objectMapper.readValue(
            "{\"primary\":\"ROOF_ASPHALTE_BITUME\",\"secondary\":\"ROOF_TUILES\"}",
            DetectedRoofCovering.class))
        .thenReturn(expectedCovering);

    Map<String, Object> result =
        subject.apply(
            feature,
            geometryUsedForAreaComputing,
            roofGeometryUsedForRateComputing,
            detectedObjects,
            null);

    assertEquals("existing_value", result.get("existing_key"));
    assertEquals(150.0, result.get("roof_area_in_m2"));
    assertEquals(expectedAddresses, result.get("addresses"));
    assertEquals(VegetationIndex.FAIBLE, result.get("vegetation_index"));
    assertEquals(FireRiskLevel.MODERE, result.get("fire_risk"));
    assertEquals(MaintenancePriority.PRIORITAIRE, result.get("maintenance_vegetation"));
    assertEquals("ROOF_ASPHALTE_BITUME", result.get("revetement_1"));
    assertEquals("ROOF_TUILES", result.get("revetement_2"));

    verify(roofAssessmentFacade).computeAssessment(evaluatorCaptor.capture());
    RoofVegetationContextEvaluator evaluator = evaluatorCaptor.getValue();
    assertEquals(CoveringType.HIGH_COMBUSTIBILITY, evaluator.getRoofContext().coveringType());
  }

  @Test
  void should_handle_null_feature_properties() {
    feature.setProperties(null);

    Map<String, Object> result =
        subject.apply(
            feature,
            geometryUsedForAreaComputing,
            roofGeometryUsedForRateComputing,
            detectedObjects,
            null);

    assertEquals(150.0, result.get("roof_area_in_m2"));
    assertNull(result.get("addresses"));
    assertFalse(result.containsKey("revetement_1"));
    assertFalse(result.containsKey("revetement_2"));

    verify(roofAssessmentFacade).computeAssessment(evaluatorCaptor.capture());
    RoofVegetationContextEvaluator evaluator = evaluatorCaptor.getValue();
    // Unknown covering is conservatively treated as highly combustible for fire risk
    assertEquals(CoveringType.HIGH_COMBUSTIBILITY, evaluator.getRoofContext().coveringType());
  }

  @Test
  void should_classify_non_bitumen_covering_as_low_combustibility() throws Exception {
    Map<String, Object> inputProps = new HashMap<>();
    inputProps.put("covering", "{\"primary\":\"ROOF_TUILES\"}");
    feature.setProperties(inputProps);

    DetectedRoofCovering covering = new DetectedRoofCovering(RoofCoveringType.ROOF_TUILES, null);
    when(objectMapper.readValue(anyString(), eq(DetectedRoofCovering.class))).thenReturn(covering);

    Map<String, Object> result =
        subject.apply(
            feature,
            geometryUsedForAreaComputing,
            roofGeometryUsedForRateComputing,
            detectedObjects,
            null);

    assertEquals("ROOF_TUILES", result.get("revetement_1"));
    assertNull(result.get("revetement_2"));

    verify(roofAssessmentFacade).computeAssessment(evaluatorCaptor.capture());
    assertEquals(
        CoveringType.LOW_COMBUSTIBILITY,
        evaluatorCaptor.getValue().getRoofContext().coveringType());
  }

  @Test
  void should_handle_json_processing_exceptions() throws Exception {
    Map<String, Object> inputProps = new HashMap<>();
    inputProps.put("addresses", "invalid-json");
    inputProps.put("covering", "invalid-json");
    feature.setProperties(inputProps);

    when(objectMapper.readValue(eq("invalid-json"), any(TypeReference.class)))
        .thenThrow(new JsonProcessingException("parse error") {});
    when(objectMapper.readValue("invalid-json", DetectedRoofCovering.class))
        .thenThrow(new JsonProcessingException("parse error") {});

    Map<String, Object> result =
        subject.apply(
            feature,
            geometryUsedForAreaComputing,
            roofGeometryUsedForRateComputing,
            detectedObjects,
            null);

    assertNull(result.get("addresses"));
    assertFalse(result.containsKey("revetement_1"));
    assertFalse(result.containsKey("revetement_2"));
  }

  @Test
  void should_handle_null_primary_in_covering() throws Exception {
    Map<String, Object> inputProps = new HashMap<>();
    inputProps.put("covering", "{\"primary\":null}");
    feature.setProperties(inputProps);

    DetectedRoofCovering covering = new DetectedRoofCovering(null, null);
    when(objectMapper.readValue(anyString(), eq(DetectedRoofCovering.class))).thenReturn(covering);

    Map<String, Object> result =
        subject.apply(
            feature,
            geometryUsedForAreaComputing,
            roofGeometryUsedForRateComputing,
            detectedObjects,
            null);

    assertNull(result.get("revetement_1"));
    assertNull(result.get("revetement_2"));

    verify(roofAssessmentFacade).computeAssessment(evaluatorCaptor.capture());
    // Unknown covering is conservatively treated as highly combustible for fire risk
    assertEquals(
        CoveringType.HIGH_COMBUSTIBILITY,
        evaluatorCaptor.getValue().getRoofContext().coveringType());
  }

  @Test
  void should_put_mutation_property_when_mutation_context_is_provided() {
    var older =
        new AreaPictureHistoryResponse.DatedImage(
            2022, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/old.jpg"));
    var mostRecent =
        new AreaPictureHistoryResponse.DatedImage(
            2024, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/new.jpg"));
    var mutationContext = new MutationContext(older, mostRecent, new File("mask.png"));
    when(mutationComputer.apply(mutationContext)).thenReturn(MutationType.DETERIORATION);

    Map<String, Object> result =
        subject.apply(
            feature,
            geometryUsedForAreaComputing,
            roofGeometryUsedForRateComputing,
            detectedObjects,
            mutationContext);

    assertEquals(MutationType.DETERIORATION, result.get("mutation"));
    assertEquals("https://geodata.test/new.jpg", result.get("mutation_recent_image_url"));
    assertEquals(2024, result.get("mutation_recent_image_date"));
    assertEquals("https://geodata.test/old.jpg", result.get("mutation_older_image_url"));
    assertEquals(2022, result.get("mutation_older_image_date"));
  }

  @Test
  void should_put_unknown_mutation_property_instead_of_failing_when_mutation_computation_throws() {
    var older =
        new AreaPictureHistoryResponse.DatedImage(
            2022, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/old.jpg"));
    var mostRecent =
        new AreaPictureHistoryResponse.DatedImage(
            2024, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/new.jpg"));
    var mutationContext = new MutationContext(older, mostRecent, new File("mask.png"));
    when(mutationComputer.apply(mutationContext))
        .thenThrow(new IllegalStateException("mutation API unreachable"));

    Map<String, Object> result =
        subject.apply(
            feature,
            geometryUsedForAreaComputing,
            roofGeometryUsedForRateComputing,
            detectedObjects,
            mutationContext);

    assertEquals(MutationType.UNKNOWN, result.get("mutation"));
    assertEquals(150.0, result.get("roof_area_in_m2"));
  }

  @Test
  void should_put_null_image_url_when_presigned_url_is_missing_from_geodata_response() {
    var older = new AreaPictureHistoryResponse.DatedImage(2022, null);
    var mostRecent = new AreaPictureHistoryResponse.DatedImage(2024, null);
    var mutationContext = new MutationContext(older, mostRecent, new File("mask.png"));
    when(mutationComputer.apply(mutationContext)).thenReturn(MutationType.NONE);

    Map<String, Object> result =
        subject.apply(
            feature,
            geometryUsedForAreaComputing,
            roofGeometryUsedForRateComputing,
            detectedObjects,
            mutationContext);

    assertNull(result.get("mutation_recent_image_url"));
    assertNull(result.get("mutation_older_image_url"));
    assertEquals(2024, result.get("mutation_recent_image_date"));
    assertEquals(2022, result.get("mutation_older_image_date"));
  }

  @Test
  void should_put_unknown_mutation_property_when_mutation_context_is_null() {
    Map<String, Object> result =
        subject.apply(
            feature,
            geometryUsedForAreaComputing,
            roofGeometryUsedForRateComputing,
            detectedObjects,
            null);

    assertEquals(MutationType.UNKNOWN, result.get("mutation"));
    assertFalse(result.containsKey("mutation_recent_image_url"));
    assertFalse(result.containsKey("mutation_older_image_url"));
    verifyNoInteractions(mutationComputer);
  }

  private static Geometry createSquare(GeometryFactory gf, double size) {
    return gf.createPolygon(
        new Coordinate[] {
          new Coordinate(0, 0),
          new Coordinate(size, 0),
          new Coordinate(size, size),
          new Coordinate(0, size),
          new Coordinate(0, 0)
        });
  }
}
