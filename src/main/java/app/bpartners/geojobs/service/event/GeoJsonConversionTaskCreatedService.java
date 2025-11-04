package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionTaskCreated;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import app.bpartners.geojobs.service.TaskConsumer;
import org.springframework.stereotype.Service;

@Service
public class GeoJsonConversionTaskCreatedService
    extends TaskCreatedService<GeoJsonConversionTask, GeoJsonConversionTaskCreated> {
  public GeoJsonConversionTaskCreatedService(
      TaskConsumer<GeoJsonConversionTask> geoJsonConversionTaskTaskConsumer,
      TaskStatusService<GeoJsonConversionTask> taskStatusService,
      TaskRepository<GeoJsonConversionTask> taskRepository) {
    super(geoJsonConversionTaskTaskConsumer, taskStatusService, taskRepository);
  }

  @Override
  public void accept(GeoJsonConversionTaskCreated geoJsonConversionTaskCreated) {
    super.accept(geoJsonConversionTaskCreated);
  }
}
