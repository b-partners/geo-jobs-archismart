package app.bpartners.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.endpoint.rest.model.ProcessJobAnnotation;
import app.bpartners.geojobs.job.service.JobAnnotationService;
import app.bpartners.geojobs.model.exception.BadRequestException;
import org.junit.jupiter.api.Test;

public class JobControllerTest {
  JobAnnotationService annotationServiceMock = mock();
  JobController subject = new JobController(annotationServiceMock);

  @Test
  void provide_bad_confidence_ko() {
    assertThrows(
        BadRequestException.class,
        () ->
            subject.processAnnotationJob(
                "jobId", new ProcessJobAnnotation().minimumConfidence(null)));
    assertThrows(
        BadRequestException.class,
        () ->
            subject.processAnnotationJob(
                "jobId", new ProcessJobAnnotation().minimumConfidence(1.01)));
    assertThrows(
        BadRequestException.class,
        () ->
            subject.processAnnotationJob(
                "jobId", new ProcessJobAnnotation().minimumConfidence(-0.01)));
  }
}
