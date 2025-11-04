package app.bpartners.geojobs.service;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import org.springframework.stereotype.Service;

@Service
public class DetectionAddressConversionTaskStatusService
    extends TaskStatusService<DetectionAddressConversionTask> {
  public DetectionAddressConversionTaskStatusService(TaskStatusRepository taskStatusRepository) {
    super(taskStatusRepository);
  }
}
