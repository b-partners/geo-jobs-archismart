package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionType;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import org.springframework.stereotype.Component;

@Component
public class ZoneDetectionTypeMapper {
  public ZoneDetectionType toRest(ZoneDetectionJob.DetectionType domain) {
    if (domain == null) return null;
    switch (domain) {
      case HUMAN -> {
        return ZoneDetectionType.HUMAN;
      }
      case MACHINE -> {
        return ZoneDetectionType.MACHINE;
      }
      default -> throw new NotImplementedException("Unknown zone detection type " + domain);
    }
  }
}
