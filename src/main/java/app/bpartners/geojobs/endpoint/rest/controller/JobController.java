package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.model.AnnotationJobProcessing;
import app.bpartners.geojobs.endpoint.rest.model.ProcessJobAnnotation;
import app.bpartners.geojobs.job.service.JobAnnotationService;
import app.bpartners.geojobs.model.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class JobController {
  private final JobAnnotationService jobAnnotationService;

  @PostMapping("/jobs/{id}/annotationProcessing")
  public AnnotationJobProcessing processAnnotationJob(
      @PathVariable String id, @RequestBody ProcessJobAnnotation processJobAnnotation) {
    Double minimumConfidence = processJobAnnotation.getMinimumConfidence();
    if (minimumConfidence == null) {
      throw new BadRequestException("MinimumConfidence is mandatory");
    } else if (minimumConfidence > 1 || minimumConfidence < 0) {
      throw new BadRequestException(
          "MinimumConfidence must be between 0 and 1, otherwise its provided value is "
              + minimumConfidence);
    }
    return jobAnnotationService.processAnnotationJob(id, minimumConfidence);
  }
}
