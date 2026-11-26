package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.HealthStatus.SUCCEEDED;
import static java.time.ZoneOffset.UTC;
import static org.springframework.data.domain.Pageable.unpaged;

import app.bpartners.geojobs.endpoint.rest.model.DetectionExportAttribute;
import app.bpartners.geojobs.endpoint.rest.model.HealthStatus;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CityJSONRequestExportService {
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final CityJSONRequestRepository cityJSONRequestRepository;
  private final CityJSONRequestConverter cityJSONRequestConverter;
  private final BucketComponent bucketComponent;

  public String requestExport(
      String communityOwnerIdentifier,
      String exportRequestIdentifier,
      LocalDate from,
      LocalDate to,
      List<DetectionExportAttribute> additionalAttributes,
      List<HealthStatus> requestStatuses) {
    var community =
        communityAuthorizationRepository.findById(communityOwnerIdentifier).orElseThrow();

    var statuses =
        requestStatuses == null || requestStatuses.isEmpty()
            ? Set.of(SUCCEEDED)
            : Set.copyOf(requestStatuses);
    var fromInstant = from.atStartOfDay().toInstant(UTC);
    var toInstant = to.atStartOfDay().plusDays(1L).minusSeconds(1L).toInstant(UTC);

    var requests =
        Set.of(SUCCEEDED).equals(statuses)
            ? cityJSONRequestRepository.findSucceededByCommunityOwnerIdAndCriteria(
                community.getId(), fromInstant, toInstant, unpaged())
            : cityJSONRequestRepository
                .findByCommunityOwnerIdAndCriteria(
                    community.getId(), fromInstant, toInstant, unpaged())
                .stream()
                .filter(request -> statuses.contains(request.exportStatus()))
                .toList();

    var exportedFile = cityJSONRequestConverter.apply(requests, additionalAttributes);

    bucketComponent.upload(exportedFile, exportRequestIdentifier);

    return bucketComponent.presign(exportRequestIdentifier);
  }
}
