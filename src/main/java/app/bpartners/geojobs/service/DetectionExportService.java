package app.bpartners.geojobs.service;

import static java.time.ZoneOffset.UTC;
import static org.springframework.data.domain.Pageable.unpaged;

import app.bpartners.geojobs.endpoint.rest.model.DetectionExportAttribute;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import java.time.LocalDate;
import java.util.List;
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
      List<DetectionExportAttribute> additionalAttributes) {
    var community =
        communityAuthorizationRepository.findById(communityOwnerIdentifier).orElseThrow();

    var detections =
        detectionRepository.findByCommunityOwnerIdAndCriteria(
            community.getId(),
            from.atStartOfDay().toInstant(UTC),
            to.atStartOfDay().plusDays(1L).minusSeconds(1L).toInstant(UTC),
            unpaged());

    var exportedDetectionsFile = detectionConverter.apply(detections, additionalAttributes);

    bucketComponent.upload(exportedDetectionsFile, exportRequestIdentifier);

    return bucketComponent.presign(exportRequestIdentifier);
  }
}
