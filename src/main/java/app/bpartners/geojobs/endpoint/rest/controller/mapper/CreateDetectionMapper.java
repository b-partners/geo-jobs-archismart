package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetectionDebugMode;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class CreateDetectionMapper {

  @Nullable
  public CreateDetection fromDebugMode(CreateDetectionDebugMode createDetectionDebugMode) {
    return createDetectionDebugMode == null
        ? null
        : new CreateDetection()
            .emailReceiver(createDetectionDebugMode.getEmailReceiver())
            .zoneName(createDetectionDebugMode.getZoneName())
            .geoJsonOutput(createDetectionDebugMode.getGeoJsonOutput())
            .needsImageOutput(createDetectionDebugMode.getNeedsImageOutput())
            .geoJsonZone(createDetectionDebugMode.getGeoJsonZone())
            .geoServerProperties(createDetectionDebugMode.getGeoServerProperties())
            .detectableObjectModelList(createDetectionDebugMode.getDetectableObjectModelList())
            .detectableObjectModel(createDetectionDebugMode.getDetectableObjectModel())
            .geoJsonDelimitationType(createDetectionDebugMode.getGeoJsonDelimitationType())
            .toNotify(createDetectionDebugMode.getToNotify());
  }
}
