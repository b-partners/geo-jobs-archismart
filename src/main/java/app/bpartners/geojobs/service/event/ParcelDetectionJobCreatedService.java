package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.status.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskCreated;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ParcelDetectionJobCreatedService implements Consumer<ParcelDetectionJobCreated> {
  private final TileDetectionTaskRepository tileDetectionTaskRepository;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final EventProducer eventProducer;
  private final DetectionRepository detectionRepository;

  @Override
  public void accept(ParcelDetectionJobCreated parcelDetectionJobCreated) {
    var parcelDetectionJob = parcelDetectionJobCreated.getParcelDetectionJob();
    var zdjId = parcelDetectionJobCreated.getZdjId();
    var jobId = parcelDetectionJob.getId();
    var tileDetectionTasks = tileDetectionTaskRepository.findAllByJobId(jobId);
    var persistedObjectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(zdjId);
    var optionalDetection = detectionRepository.findByZdjId(zdjId);
    var detectableObjectConfigurations =
        persistedObjectConfigurations.isEmpty()
            ? (optionalDetection
                .map(Detection::getDetectableObjectConfigurations)
                .orElseGet(List::of))
            : persistedObjectConfigurations;
    var address = parcelDetectionJob.getAddress();
    var point = parcelDetectionJob.getPoint();

    eventProducer.accept(
        List.of(new ParcelDetectionStatusRecomputingSubmitted(parcelDetectionJob.getId())));
    tileDetectionTasks.forEach(
        tileDetectionTask ->
            eventProducer.accept(
                List.of(
                    new TileDetectionTaskCreated(
                        zdjId,
                        tileDetectionTask,
                        detectableObjectConfigurations,
                        address,
                        point))));
  }
}
