package app.bpartners.geojobs.endpoint.rest.validator;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.VEGETATION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.service.DetectionImageZoomResolver;
import app.bpartners.geojobs.validator.CreateDetectionValidator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateDetectionValidatorTest {

  DetectionImageZoomResolver detectionImageZoomResolverMock = mock();
  CreateDetectionValidator subject = new CreateDetectionValidator(detectionImageZoomResolverMock);

  @BeforeEach
  void setUp() {
    when(detectionImageZoomResolverMock.toitureZoom()).thenReturn(21);
  }

  @Test
  void do_not_throws_create_detection_with_mandatory_fields() {
    var createDetectionWithMandatoryFields =
        new CreateDetection()
            .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
            .detectableObjectModelList(null)
            .emailReceiver("email@receiver.com")
            .zoneName("dummy zone name");
    var createDetectionWithMandatoryFieldsWithEmptyDetectableModelList =
        new CreateDetection()
            .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
            .detectableObjectModelList(List.of())
            .emailReceiver("email@receiver.com")
            .zoneName("dummy zone name");
    var createDetectionWithMandatoryFieldsUsingDetectableModelList =
        new CreateDetection()
            .detectableObjectModel(null)
            .detectableObjectModelList(List.of(new DetectableObjectModel().modelName(TOITURE)))
            .emailReceiver("email@receiver.com")
            .zoneName("dummy zone name");

    assertDoesNotThrow(() -> subject.accept(createDetectionWithMandatoryFields));
    assertDoesNotThrow(
        () -> subject.accept(createDetectionWithMandatoryFieldsWithEmptyDetectableModelList));
    assertDoesNotThrow(
        () -> subject.accept(createDetectionWithMandatoryFieldsUsingDetectableModelList));
  }

  @Test
  void throws_bad_request_exception_when_mandatory_fields_missing() {
    var actual =
        assertThrows(BadRequestException.class, () -> subject.accept(new CreateDetection()));
    var actualWithMissingModelName =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    new CreateDetection().detectableObjectModel(new DetectableObjectModel())));

    var expectedMessage =
        "CreateDetection.emailReceiver is mandatory. CreateDetection.zoneName is mandatory. Either"
            + " CreateDetection.detectableObjectModel or CreateDetection.detectableObjectModelList"
            + " is mandatory.";
    assertEquals(expectedMessage, actual.getMessage());
    assertEquals(expectedMessage, actualWithMissingModelName.getMessage());
  }

  @Test
  void throws_not_implemented_when_a_feature_provides_a_zoom_other_than_the_legacy_one() {
    var createDetectionWithZoom = validCreateDetection().geoJsonZone(List.of(featureWithZoom(21)));

    assertThrows(NotImplementedException.class, () -> subject.accept(createDetectionWithZoom));
  }

  @Test
  void accepts_legacy_zoom_20_for_toiture_only_and_converts_it_to_the_resolved_zoom() {
    var feature = featureWithZoom(20);
    var createDetection = validCreateDetection().geoJsonZone(List.of(feature));

    assertDoesNotThrow(() -> subject.accept(createDetection));

    assertEquals(21, feature.getProperties().get("zoom"));
    assertEquals("a", feature.getProperties().get("id"));
  }

  @Test
  void accepts_legacy_zoom_20_for_toiture_only_provided_through_the_model_list() {
    var feature = featureWithZoom(20);
    var createDetection =
        validCreateDetection()
            .detectableObjectModel(null)
            .detectableObjectModelList(List.of(new DetectableObjectModel().modelName(TOITURE)))
            .geoJsonZone(List.of(feature));

    assertDoesNotThrow(() -> subject.accept(createDetection));

    assertEquals(21, feature.getProperties().get("zoom"));
  }

  @Test
  void throws_not_implemented_when_legacy_zoom_20_is_provided_with_another_model_than_toiture() {
    var createDetection =
        validCreateDetection()
            .detectableObjectModel(null)
            .detectableObjectModelList(
                List.of(
                    new DetectableObjectModel().modelName(TOITURE),
                    new DetectableObjectModel().modelName(VEGETATION)))
            .geoJsonZone(List.of(featureWithZoom(20)));

    assertThrows(NotImplementedException.class, () -> subject.accept(createDetection));
  }

  @Test
  void throws_not_implemented_when_only_some_features_provide_the_legacy_zoom() {
    var createDetection =
        validCreateDetection().geoJsonZone(List.of(featureWithZoom(20), featureWithZoom(21)));

    assertThrows(NotImplementedException.class, () -> subject.accept(createDetection));
  }

  @Test
  void do_not_throws_when_features_do_not_provide_any_zoom() {
    var feature = new Feature().properties(new HashMap<>(Map.of("id", "a")));
    var createDetection = validCreateDetection().geoJsonZone(List.of(feature));

    assertDoesNotThrow(() -> subject.accept(createDetection));

    assertNull(feature.getProperties().get("zoom"));
  }

  private Feature featureWithZoom(int zoom) {
    return new Feature().properties(new HashMap<>(Map.of("id", "a", "zoom", zoom)));
  }

  private CreateDetection validCreateDetection() {
    return new CreateDetection()
        .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
        .emailReceiver("email@receiver.com")
        .zoneName("dummy zone name");
  }
}
