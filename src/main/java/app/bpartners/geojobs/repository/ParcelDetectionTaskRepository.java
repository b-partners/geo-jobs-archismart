package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import org.springframework.stereotype.Repository;

@Repository
public interface ParcelDetectionTaskRepository extends TaskRepository<ParcelDetectionTask> {}
