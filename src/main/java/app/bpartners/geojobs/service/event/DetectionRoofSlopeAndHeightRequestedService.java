package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.repository.DetectionRepository;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionRoofSlopeAndHeightRequestedService
    implements Consumer<DetectionRoofSlopeAndHeightRequested> {
  public static final String ROOF_SLOPE_PROPERTY_NAME = "roof_slope_in_degrees";
  public static final String ROOF_HEIGHT_PROPERTY_NAME = "roof_height_in_meters";
  public static final String LIDAR_DATA_STATUS_PROPERTY_NAME = "lidar_data_status";
  private final DetectionRepository detectionRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(DetectionRoofSlopeAndHeightRequested requested) {
    var detectionIdentifier = requested.getDetectionId();
    var detection =
        detectionRepository
            .findById(detectionIdentifier)
            .orElseThrow(
                () -> new RuntimeException("Detection={" + detectionIdentifier + "} not found"));

    var detectionSaved =
        detectionRepository.save(
            detection.toBuilder().roofPropertiesComputationCreationDatetime(now()).build());

    var featureWithDelimitations = detectionSaved.getFeatureWithDelimitations();
    if (featureWithDelimitations == null) {
      throw new IllegalArgumentException(
          "FeatureWithDelimitation is null for detection={" + detectionIdentifier + "}");
    }

    var providedGeoJsonZone = detectionSaved.getProvidedGeoJsonZone();
    providedGeoJsonZone.forEach(
        feature ->
            eventProducer.accept(
                List.of(new FeatureRoofSlopeAndHeightRequested(detectionIdentifier, feature))));
  }
}
