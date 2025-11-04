package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingTask;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnotationRetrievingTaskRepository
    extends TaskRepository<AnnotationRetrievingTask> {
  Optional<AnnotationRetrievingTask> findByAnnotationTaskId(String annotationTaskId);

  List<AnnotationRetrievingTask> findByJobId(String jobId);
}
