package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.io.File;
import java.util.List;

public interface TileObjectDetector {
  DetectionResponseV2 apply(
      TileDetectionTask tileDetectionTask,
      File mask,
      List<DetectableObjectConfiguration> detectableObjectConfigurations,
      List<DetectableObjectModel> detectableObjectModels);
}
