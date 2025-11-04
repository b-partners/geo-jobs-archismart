package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionJob;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import org.springframework.stereotype.Service;

@Service
public class DetectionAddressConversionJobStatusRecomputingSubmittedBean
    extends JobStatusRecomputingSubmittedService<
        DetectionAddressConversionJob,
        DetectionAddressConversionTask,
        DetectionAddressConversionJobStatusRecomputingSubmitted> {
  public DetectionAddressConversionJobStatusRecomputingSubmittedBean(
      JobService<DetectionAddressConversionTask, DetectionAddressConversionJob> jobService,
      TaskStatusService<DetectionAddressConversionTask> taskStatusService,
      TaskRepository<DetectionAddressConversionTask> taskRepository) {
    super(jobService, taskStatusService, taskRepository);
  }
}
