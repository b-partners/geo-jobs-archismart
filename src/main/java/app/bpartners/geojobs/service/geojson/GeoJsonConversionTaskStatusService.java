package app.bpartners.geojobs.service.geojson;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import org.springframework.stereotype.Service;

@Service
public class GeoJsonConversionTaskStatusService extends TaskStatusService<GeoJsonConversionTask> {
  public GeoJsonConversionTaskStatusService(TaskStatusRepository taskStatusRepository) {
    super(taskStatusRepository);
  }
}
