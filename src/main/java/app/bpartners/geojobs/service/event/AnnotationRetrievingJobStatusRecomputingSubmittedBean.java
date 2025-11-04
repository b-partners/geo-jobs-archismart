package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.repository.AnnotationRetrievingTaskRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingJob;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingTask;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.annotator.AnnotationRetrievingTaskStatusService;
import org.springframework.stereotype.Service;

@Service
public class AnnotationRetrievingJobStatusRecomputingSubmittedBean
    extends JobStatusRecomputingSubmittedService<
        AnnotationRetrievingJob,
        AnnotationRetrievingTask,
        AnnotationRetrievingJobStatusRecomputingSubmitted> {
  public AnnotationRetrievingJobStatusRecomputingSubmittedBean(
      AnnotationRetrievingJobService jobService,
      AnnotationRetrievingTaskStatusService taskStatusService,
      AnnotationRetrievingTaskRepository taskRepository) {
    super(jobService, taskStatusService, taskRepository);
  }
}
