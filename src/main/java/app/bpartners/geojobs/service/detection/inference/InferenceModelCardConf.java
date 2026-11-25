package app.bpartners.geojobs.service.detection.inference;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.service.detection.ModelVersionConf;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InferenceModelCardConf {
  private final ModelVersionConf modelVersionConf;

  public InferenceModelCardConf(ModelVersionConf modelVersionConf) {
    this.modelVersionConf = modelVersionConf;
  }

  public List<ModelCardMapping> getMappings() {
    return modelVersionConf.getInferenceMappings(List.of());
  }

  public List<ModelCardMapping> getMappings(List<DetectableObjectModel> models) {
    return modelVersionConf.getInferenceMappings(models);
  }
}
