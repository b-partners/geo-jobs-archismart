package app.bpartners.geojobs.service.event.detection;

import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.ParcelDetectionTaskStatusService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.event.JobStatusRecomputingSubmittedService;
import org.springframework.stereotype.Service;

@Service
public class ZDJStatusRecomputingSubmittedBean
    extends JobStatusRecomputingSubmittedService<
        ZoneDetectionJob, ParcelDetectionTask, ZDJStatusRecomputingSubmitted> {
  public ZDJStatusRecomputingSubmittedBean(
      ZoneDetectionJobService jobService,
      ParcelDetectionTaskStatusService taskStatusService,
      ParcelDetectionTaskRepository taskRepository) {
    super(jobService, taskStatusService, taskRepository);
  }
}
