package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.*;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionSupportedModelValidator implements Consumer<Detection> {
  public static final List<ModelName> UNSUPPORTED_MODELS =
      List.of(VOIRIE_TROTTOIRS, TAMPONS, SIGN, CIMETIERE, OLD, CYCL);

  @Override
  public void accept(Detection detection) {
    var detectableObjectModelList = detection.getDetectableObjectModelList();
    if ((detection.getDetectableObjectModel() == null
            || detection.getDetectableObjectModel().getModelName() == null)
        && (detectableObjectModelList == null || detectableObjectModelList.isEmpty())) {
      return;
    }
    if (detectableObjectModelList != null && !detectableObjectModelList.isEmpty()) {
      var actualModelNames =
          detectableObjectModelList.stream().map(DetectableObjectModel::getModelName).toList();
      var unsupportedModelsOnModelList =
          actualModelNames.stream().filter(UNSUPPORTED_MODELS::contains).toList();
      if (!unsupportedModelsOnModelList.isEmpty()) {
        throw new UnsupportedOperationException(
            "Detection has unsupported models "
                + unsupportedModelsOnModelList.stream().map(ModelName::toString).toList());
      }
    }
    var detectionModelName = detection.getDetectableObjectModel().getModelName();
    if (UNSUPPORTED_MODELS.contains(detectionModelName)) {
      throw new UnsupportedOperationException(
          "Detection has unsupported model " + detectionModelName);
    }
  }
}
