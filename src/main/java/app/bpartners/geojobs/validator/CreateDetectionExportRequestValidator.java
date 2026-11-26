package app.bpartners.geojobs.endpoint.rest.validator;

import static app.bpartners.geojobs.endpoint.rest.model.HealthStatus.SUCCEEDED;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetectionExportRequest;
import app.bpartners.geojobs.model.exception.BadRequestException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateDetectionExportRequestValidator
    implements Consumer<CreateDetectionExportRequest> {
  @Override
  public void accept(CreateDetectionExportRequest createDetectionExportRequest) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (createDetectionExportRequest.getFrom() == null) {
      exceptionMessageBuilder.append("CreateDetectionExportRequest.from is mandatory. ");
    }
    if (createDetectionExportRequest.getTo() == null) {
      exceptionMessageBuilder.append("CreateDetectionExportRequest.to is mandatory. ");
    }
    if (createDetectionExportRequest.getAdditionalExportedAttributes() != null) {
      if (createDetectionExportRequest.getAdditionalExportedAttributes().isEmpty()) {
        exceptionMessageBuilder.append(
            "CreateDetectionExportRequest.additionalExportedAttributes can not be empty. ");
      } else {
        if (createDetectionExportRequest.getAdditionalExportedAttributes().stream()
            .anyMatch(detectionExportAttribute -> detectionExportAttribute == null)) {
          exceptionMessageBuilder.append(
              "CreateDetectionExportRequest.additionalExportedAttributes can not contain null"
                  + " value. ");
        }
      }
    }
    var detectionStatuses = createDetectionExportRequest.getDetectionStatuses();
    if (detectionStatuses != null && detectionStatuses.stream().anyMatch(Objects::isNull)) {
      exceptionMessageBuilder.append(
          "CreateDetectionExportRequest.detectionStatuses can not contain null value. ");
    }
    var exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
    if (detectionStatuses == null || detectionStatuses.isEmpty()) {
      createDetectionExportRequest.setDetectionStatuses(List.of(SUCCEEDED));
    }
  }
}
