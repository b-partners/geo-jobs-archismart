package app.bpartners.geojobs.endpoint.rest.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetectionExportRequest;
import app.bpartners.geojobs.endpoint.rest.model.DetectionExportAttribute;
import app.bpartners.geojobs.model.exception.BadRequestException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateDetectionExportRequestValidatorTest {
  CreateDetectionExportRequestValidator subject = new CreateDetectionExportRequestValidator();

  @Test
  void accept_without_exception() {
    var request =
        new CreateDetectionExportRequest()
            .from(LocalDate.of(2024, 1, 1))
            .to(LocalDate.of(2024, 12, 31))
            .additionalExportedAttributes(List.of(DetectionExportAttribute.AREA));

    assertDoesNotThrow(() -> subject.accept(request));
  }

  @Test
  void accept_without_exception_when_additional_attributes_null() {
    var request =
        new CreateDetectionExportRequest()
            .from(LocalDate.of(2024, 1, 1))
            .to(LocalDate.of(2024, 12, 31))
            .additionalExportedAttributes(null);

    assertDoesNotThrow(() -> subject.accept(request));
  }

  @Test
  void throws_mandatory_attribute_missing_exception() {
    var request =
        new CreateDetectionExportRequest().from(null).to(null).additionalExportedAttributes(null);

    var actualException = assertThrows(BadRequestException.class, () -> subject.accept(request));

    assertEquals(
        "CreateDetectionExportRequest.from is mandatory. "
            + "CreateDetectionExportRequest.to is mandatory. ",
        actualException.getMessage());
  }

  @Test
  void throws_empty_additional_attributes_exception() {
    var request =
        new CreateDetectionExportRequest()
            .from(LocalDate.of(2024, 1, 1))
            .to(LocalDate.of(2024, 12, 31))
            .additionalExportedAttributes(List.of());

    var actualException = assertThrows(BadRequestException.class, () -> subject.accept(request));

    assertEquals(
        "CreateDetectionExportRequest.additionalExportedAttributes can not be empty. ",
        actualException.getMessage());
  }

  @Test
  void throws_null_valued_additional_attributes_exception() {
    var request =
        new CreateDetectionExportRequest()
            .from(LocalDate.of(2024, 1, 1))
            .to(LocalDate.of(2024, 12, 31))
            .additionalExportedAttributes(Arrays.asList(DetectionExportAttribute.AREA, null));

    var actualException = assertThrows(BadRequestException.class, () -> subject.accept(request));

    assertEquals(
        "CreateDetectionExportRequest.additionalExportedAttributes can not contain null value. ",
        actualException.getMessage());
  }
}
