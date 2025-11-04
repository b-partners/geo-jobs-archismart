package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnotationDeliveryJobRepository
    extends JpaRepository<AnnotationDeliveryJob, String> {
  List<AnnotationDeliveryJob> findAllByDetectionJobId(String detectionJobId);

  Optional<AnnotationDeliveryJob> findByAnnotationJobId(String id);
}
