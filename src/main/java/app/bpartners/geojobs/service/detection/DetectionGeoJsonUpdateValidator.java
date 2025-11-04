package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class DetectionGeoJsonUpdateValidator implements Consumer<Detection> {
  @Override
  public void accept(Detection detection) {
    if (detection.getProvidedGeoJsonZone() != null
        && !detection.getProvidedGeoJsonZone().isEmpty()) {
      throw new BadRequestException(
          "Unable to finalize Detection(id="
              + detection.getEndToEndId()
              + ") geoJson as it already has values");
    } else if (detection.getShapeFileKey() != null || detection.getExcelFileKey() != null) {
      throw new BadRequestException(
          "Unable to configure Detection(id="
              + detection.getEndToEndId()
              + ") geoJson as it is already being configuring");
    }
  }
}
