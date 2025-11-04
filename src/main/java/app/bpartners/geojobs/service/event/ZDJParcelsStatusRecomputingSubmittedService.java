package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.status.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJParcelsStatusRecomputingSubmitted;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ZDJParcelsStatusRecomputingSubmittedService
    implements Consumer<ZDJParcelsStatusRecomputingSubmitted> {
  private final ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  private final EventProducer eventProducer;

  @Override
  @Transactional
  public void accept(ZDJParcelsStatusRecomputingSubmitted zdjParcelsStatusRecomputingSubmitted) {
    var zoneDetectionJobId = zdjParcelsStatusRecomputingSubmitted.getZoneDetectionJobId();
    var parcelDetectionTasks = parcelDetectionTaskRepository.findAllByJobId(zoneDetectionJobId);
    parcelDetectionTasks.stream()
        .filter(task -> task.getAsJobId() != null)
        .toList()
        .forEach(
            task ->
                eventProducer.accept(
                    List.of(new ParcelDetectionStatusRecomputingSubmitted(task.getAsJobId()))));
  }
}
