package app.bpartners.geojobs.repository.model.detection;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.REVETEMENT_OBSTACLE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.VEGETATION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DetectionModelNameTest {
  @Test
  void revetement_obstacle_model_is_roof_based_without_being_toiture() {
    var detection = detectionWith(REVETEMENT_OBSTACLE);

    assertTrue(detection.hasRevetementObstacleModelName());
    assertTrue(detection.hasRoofBasedModelName());
    assertFalse(detection.hasToitureModelName());
  }

  @Test
  void toiture_model_is_roof_based_without_being_revetement_obstacle() {
    var detection = detectionWith(TOITURE);

    assertTrue(detection.hasToitureModelName());
    assertTrue(detection.hasRoofBasedModelName());
    assertFalse(detection.hasRevetementObstacleModelName());
  }

  @Test
  void other_models_are_not_roof_based() {
    var detection = detectionWith(VEGETATION);

    assertFalse(detection.hasRoofBasedModelName());
    assertFalse(detection.hasRevetementObstacleModelName());
  }

  @Test
  void single_model_is_considered_too() {
    var detection =
        Detection.builder()
            .detectableObjectModel(new DetectableObjectModel().modelName(REVETEMENT_OBSTACLE))
            .build();

    assertTrue(detection.hasRoofBasedModelName());
  }

  private static Detection detectionWith(ModelName... modelNames) {
    return Detection.builder()
        .detectableObjectModelList(
            Arrays.stream(modelNames)
                .map(modelName -> new DetectableObjectModel().modelName(modelName))
                .toList())
        .build();
  }
}
