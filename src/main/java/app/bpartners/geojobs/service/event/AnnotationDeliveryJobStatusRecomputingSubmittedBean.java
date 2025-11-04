package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.repository.AnnotationDeliveryTaskRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import app.bpartners.geojobs.service.AnnotationDeliveryJobService;
import app.bpartners.geojobs.service.annotator.AnnotationDeliveryTaskStatusService;
import org.springframework.stereotype.Service;

@Service
public class AnnotationDeliveryJobStatusRecomputingSubmittedBean
    extends JobStatusRecomputingSubmittedService<
        AnnotationDeliveryJob,
        AnnotationDeliveryTask,
        AnnotationDeliveryJobStatusRecomputingSubmitted> {
  public AnnotationDeliveryJobStatusRecomputingSubmittedBean(
      AnnotationDeliveryJobService jobService,
      AnnotationDeliveryTaskStatusService taskStatusService,
      AnnotationDeliveryTaskRepository taskRepository) {
    super(jobService, taskStatusService, taskRepository);
  }
}
