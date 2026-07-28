package app.bpartners.geojobs.endpoint.rest.controller.v1.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.REQUEST_ACCEPTED;

import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionRestMapper {
  private final DetectionFromStatisticRestMapper detectionFromStatisticRestMapper;

  public app.bpartners.geojobs.endpoint.rest.model.Detection toRest(Detection detection) {
    var currentStep = detection.getCurrentStep();
    if (currentStep == null) {
      return detectionFromStatisticRestMapper.apply(
          detection, new TaskStatistic(), REQUEST_ACCEPTED);
    }
    var statistic = currentStep.getStatistic();
    if (statistic != null) {
      return detectionFromStatisticRestMapper.apply(detection, statistic, currentStep.getName());
    }
    return detectionFromStatisticRestMapper.computeEmptyStatisticFromStep(
        detection,
        currentStep.getProgression(),
        currentStep.getHealth(),
        currentStep.getMessage(),
        currentStep.getName());
  }

  public List<app.bpartners.geojobs.endpoint.rest.model.Detection> toRest(
      List<Detection> detections) {
    return detections.stream().map(this::toRest).toList();
  }
}
