package app.bpartners.geojobs.service;

import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class GeoServerLayerRetriever
    implements Function<List<DetectionAddressConversionTask>, String> {
  @Override
  public String apply(List<DetectionAddressConversionTask> tasks) {
    var layers =
        new HashSet<>(tasks.stream().map(DetectionAddressConversionTask::getLayer).toList());
    if (layers.size() > 1) {
      throw new NotImplementedException(
          "Multiple layers detected " + layers + "." + " Only one layer is supported for now.");
    }
    return layers.iterator().next();
  }
}
