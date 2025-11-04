package app.bpartners.geojobs.utils.detection;

import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import java.util.List;

public class HumanDetectionJobCreator {
  public HumanDetectionJob create(
      String id,
      String zoneDetectionJobId,
      String annotationJobId,
      List<DetectableObjectConfiguration> detectableObjectConfigurations,
      List<MachineDetectedTile> machineDetectedTiles) {
    return HumanDetectionJob.builder()
        .id(id)
        .annotationJobId(annotationJobId)
        .zoneDetectionJobId(zoneDetectionJobId)
        .detectableObjectConfigurations(detectableObjectConfigurations)
        .machineDetectedTiles(machineDetectedTiles)
        .build();
  }
}
