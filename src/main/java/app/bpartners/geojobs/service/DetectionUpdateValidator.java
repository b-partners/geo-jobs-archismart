package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Component;

@Component
public class DetectionUpdateValidator implements BiConsumer<Detection, CreateDetection> {
  @Override
  public void accept(Detection detection, CreateDetection createDetection) {
    StringBuilder messageBuilder = new StringBuilder();
    if (detection.getProvidedGeoJsonZone() != null
        && !detection.getProvidedGeoJsonZone().equals(createDetection.getGeoJsonZone())) {
      messageBuilder
          .append(
              "Detection.geoJsonZone can not be updated once it has values, otherwise actual value"
                  + " ")
          .append(detection.getProvidedGeoJsonZone())
          .append(" is not equals provided value ")
          .append(createDetection.getGeoJsonZone())
          .append(". ");
    }
    var detectableObjectModel = createDetection.getDetectableObjectModel();
    boolean detectableObjectToBeUpdated =
        detectableObjectModel != null
            && detection.getDetectableObjectModel() != null
            && !detectableObjectModel.equals(detection.getDetectableObjectModel());
    if (detectableObjectToBeUpdated) {
      messageBuilder
          .append(
              "Detection.detectableObjectModel can not be updated once it has values, otherwise"
                  + " actual value ")
          .append(detection.getDetectableObjectModel())
          .append(" is not equals provided value ")
          .append(detectableObjectModel)
          .append(". ");
    }

    String messageException = messageBuilder.toString();
    if (!messageException.isEmpty()) {
      throw new IllegalArgumentException(messageException);
    }
  }
}
