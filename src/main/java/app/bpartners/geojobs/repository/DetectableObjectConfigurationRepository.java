package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectableObjectConfigurationRepository
    extends JpaRepository<DetectableObjectConfiguration, String> {
  List<DetectableObjectConfiguration> findAllByDetectionJobId(String jobId);
}
