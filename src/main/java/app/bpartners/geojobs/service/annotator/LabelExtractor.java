package app.bpartners.geojobs.service.annotator;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.stream.Collectors.toSet;

import app.bpartners.gen.annotator.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.KeyPredicateFunction;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class LabelExtractor {
  private final KeyPredicateFunction keyPredicateFunction;
  private final LabelConverter labelConverter;

  public List<Label> createUniqueLabelListFrom(List<MachineDetectedTile> tiles) {
    return tiles.stream()
        .map(MachineDetectedTile::getDetectedObjects)
        .flatMap(Collection::stream)
        .map(DetectedObject::getDetectableObjectType)
        .distinct()
        .map(labelConverter)
        .toList();
  }

  public Label findLabelByNameFromList(List<Label> source, String labelName) {
    return source.stream()
        .filter(
            label -> {
              assert label.getName() != null;
              return label.getName().equals(labelName);
            })
        .findFirst()
        .orElseThrow(
            () ->
                new ApiException(
                    SERVER_EXCEPTION,
                    "could not find label " + labelName + " for the current job."));
  }

  public List<Label> extractLabelsFromTasks(List<CreateAnnotatedTask> annotatedTasks) {
    return annotatedTasks.stream()
        .map(CreateAnnotatedTask::getAnnotationBatch)
        .map(CreateAnnotationBatch::getAnnotations)
        .map(a -> a.stream().map(AnnotationBaseFields::getLabel).collect(toSet()))
        .reduce(
            new HashSet<>(),
            (acc, val) -> {
              acc.addAll(val);
              return acc;
            })
        .stream()
        .filter(keyPredicateFunction.apply(Label::getName))
        .toList();
  }
}
