package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.HealthStatus.SUCCEEDED;
import static java.time.ZoneOffset.UTC;
import static org.springframework.data.domain.Pageable.unpaged;

import app.bpartners.geojobs.endpoint.rest.model.DetectionExportAttribute;
import app.bpartners.geojobs.endpoint.rest.model.HealthStatus;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionExportService {
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final DetectionRepository detectionRepository;
  private final DetectionConverter detectionConverter;
  private final BucketComponent bucketComponent;

  public String requestDetectionExport(
      String communityOwnerIdentifier,
      String exportRequestIdentifier,
      LocalDate from,
      LocalDate to,
      List<DetectionExportAttribute> additionalAttributes,
      List<HealthStatus> detectionStatuses) {
    var community =
        communityAuthorizationRepository.findById(communityOwnerIdentifier).orElseThrow();

    var statuses =
        detectionStatuses == null || detectionStatuses.isEmpty()
            ? Set.of(SUCCEEDED)
            : Set.copyOf(detectionStatuses);
    var fromInstant = from.atStartOfDay().toInstant(UTC);
    var toInstant = to.atStartOfDay().plusDays(1L).minusSeconds(1L).toInstant(UTC);

    var detections =
        Set.of(SUCCEEDED).equals(statuses)
            ? detectionRepository.findSucceededByCommunityOwnerIdAndCriteria(
                community.getId(), fromInstant, toInstant, unpaged())
            : detectionRepository
                .findByCommunityOwnerIdAndCriteria(
                    community.getId(), fromInstant, toInstant, unpaged())
                .stream()
                .filter(detection -> statuses.contains(detection.exportStatus()))
                .toList();

    var exportedDetectionsFile = detectionConverter.apply(detections, additionalAttributes);

    bucketComponent.upload(exportedDetectionsFile, exportRequestIdentifier);

    return bucketComponent.presign(exportRequestIdentifier);
  }
}
