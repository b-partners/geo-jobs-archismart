package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class HumanDetectionJobService {
  private final HumanDetectionJobRepository humanDetectionJobRepository;

  @Transactional
  public HumanDetectionJob create(
      String annotationJobId,
      String humanDetectionJobId,
      List<MachineDetectedTile> machineDetectedTiles,
      String humanZDJId,
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    return humanDetectionJobRepository.save(
        HumanDetectionJob.builder()
            .id(humanDetectionJobId)
            .annotationJobId(annotationJobId)
            .machineDetectedTiles(machineDetectedTiles)
            .zoneDetectionJobId(humanZDJId)
            .detectableObjectConfigurations(detectableObjectConfigurations)
            .build());
  }
}
