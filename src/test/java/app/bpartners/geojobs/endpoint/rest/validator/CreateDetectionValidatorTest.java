package app.bpartners.geojobs.endpoint.rest.validator;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.validator.CreateDetectionValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateDetectionValidatorTest {

  CreateDetectionValidator subject = new CreateDetectionValidator();

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
}
