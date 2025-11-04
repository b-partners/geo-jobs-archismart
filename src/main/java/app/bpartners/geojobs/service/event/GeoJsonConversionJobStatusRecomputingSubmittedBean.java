package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.repository.GeoJsonConversionTaskRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionTaskStatusService;
import org.springframework.stereotype.Service;

@Service
public class GeoJsonConversionJobStatusRecomputingSubmittedBean
    extends JobStatusRecomputingSubmittedService<
        GeoJsonConversionJob,
        GeoJsonConversionTask,
        GeoJsonConversionJobStatusRecomputingSubmitted> {
  public GeoJsonConversionJobStatusRecomputingSubmittedBean(
      GeoJsonConversionJobService jobService,
      GeoJsonConversionTaskStatusService taskStatusService,
      GeoJsonConversionTaskRepository taskRepository) {
    super(jobService, taskStatusService, taskRepository);
  }
}
