package app.bpartners.geojobs.service.annotator;

import app.bpartners.gen.annotator.endpoint.rest.model.CreateAnnotatedTask;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import java.util.List;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TaskExtractor
    implements TriFunction<
        List<MachineDetectedTile>, String, List<Label>, List<CreateAnnotatedTask>> {
  private final CreateAnnotationBatchExtractor createAnnotationBatchExtractor;
  private final LabelExtractor labelExtractor;

  private CreateAnnotatedTask annotatedTaskFrom(
      MachineDetectedTile machineDetectedTile, String annotatorId, List<Label> existingLabels) {
    return new CreateAnnotatedTask()
        .id(machineDetectedTile.getId())
        .annotatorId(annotatorId)
        .filename(machineDetectedTile.getBucketPath())
        .annotationBatch(
            createAnnotationBatchExtractor.apply(machineDetectedTile, annotatorId, existingLabels));
  }

  @Override
  public List<CreateAnnotatedTask> apply(
      List<MachineDetectedTile> machineDetectedTiles,
      String annotatorId,
      List<Label> expectedLabels) {
    var existingLabels = labelExtractor.createUniqueLabelListFrom(machineDetectedTiles);
    return machineDetectedTiles.stream()
        .map(
            tile ->
                annotatedTaskFrom(
                    tile, annotatorId, existingLabels.isEmpty() ? expectedLabels : existingLabels))
        .toList();
  }
}
