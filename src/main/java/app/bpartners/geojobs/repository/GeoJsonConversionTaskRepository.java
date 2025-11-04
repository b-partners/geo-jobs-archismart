package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import org.springframework.stereotype.Repository;

@Repository
public interface GeoJsonConversionTaskRepository extends TaskRepository<GeoJsonConversionTask> {}
