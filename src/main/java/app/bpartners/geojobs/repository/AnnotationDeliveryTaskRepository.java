package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnotationDeliveryTaskRepository extends TaskRepository<AnnotationDeliveryTask> {
  Optional<AnnotationDeliveryTask> findByAnnotationTaskId(String annotationTaskId);

  List<AnnotationDeliveryTask> findByJobId(String jobId);
}
