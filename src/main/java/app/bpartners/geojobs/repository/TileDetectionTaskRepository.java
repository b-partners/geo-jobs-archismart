package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import org.springframework.stereotype.Repository;

@Repository
public interface TileDetectionTaskRepository extends TaskRepository<TileDetectionTask> {}
