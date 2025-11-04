package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HumanDetectionJobRepository extends JpaRepository<HumanDetectionJob, String> {
  List<HumanDetectionJob> findAllByZoneDetectionJobId(String jobId);

  Optional<HumanDetectionJob> findByAnnotationJobId(String annotationJobId);
}
