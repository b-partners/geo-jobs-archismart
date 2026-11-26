package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.HealthStatus.FAILED;
import static app.bpartners.geojobs.endpoint.rest.model.HealthStatus.SUCCEEDED;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Pageable.unpaged;

import app.bpartners.geojobs.endpoint.rest.model.DetectionExportAttribute;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CityJSONRequestExportServiceTest {
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  CityJSONRequestRepository cityJSONRequestRepositoryMock = mock();
  CityJSONRequestConverter cityJSONRequestConverterMock = mock();
  BucketComponent bucketComponentMock = mock();

  CityJSONRequestExportService subject =
      new CityJSONRequestExportService(
          communityAuthorizationRepositoryMock,
          cityJSONRequestRepositoryMock,
          cityJSONRequestConverterMock,
          bucketComponentMock);

  String communityOwnerId = "community_owner_id";
  String communityInternalId = "community_internal_id";
  LocalDate from = LocalDate.of(2024, 1, 1);
  LocalDate to = LocalDate.of(2024, 1, 31);
  List<DetectionExportAttribute> additionalAttributes = List.of(DetectionExportAttribute.AREA);
  File exportedFile = new File("exported_requests.xlsx");

  @Test
  void request_export_ok_returns_presigned_url_of_succeeded_requests_by_default() {
    var exportId = randomUUID().toString();
    var requests = List.of(new CityJSONRequest());
    var presignUrl = "https://bucket/presigned-url";
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(CommunityAuthorization.builder().id(communityInternalId).build()));
    when(cityJSONRequestRepositoryMock.findSucceededByCommunityOwnerIdAndCriteria(
            eq(communityInternalId),
            eq(from.atStartOfDay().toInstant(UTC)),
            eq(to.atStartOfDay().plusDays(1L).minusSeconds(1L).toInstant(UTC)),
            eq(unpaged())))
        .thenReturn(requests);
    when(cityJSONRequestConverterMock.apply(requests, additionalAttributes))
        .thenReturn(exportedFile);
    when(bucketComponentMock.presign(exportId)).thenReturn(presignUrl);

    var actual =
        subject.requestExport(communityOwnerId, exportId, from, to, additionalAttributes, null);

    assertEquals(presignUrl, actual);
    verify(bucketComponentMock).upload(exportedFile, exportId);
    verify(cityJSONRequestRepositoryMock, never())
        .findByCommunityOwnerIdAndCriteria(any(), any(), any(), any());
  }

  @Test
  void request_export_treats_requests_not_finished_as_failed() {
    var exportId = randomUUID().toString();
    var finished =
        CityJSONRequest.builder().id("finished").status(CityJSONRequestStatus.FINISHED).build();
    var processing =
        CityJSONRequest.builder().id("processing").status(CityJSONRequestStatus.PROCESSING).build();
    var failed =
        CityJSONRequest.builder().id("failed").status(CityJSONRequestStatus.FAILED).build();
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(CommunityAuthorization.builder().id(communityInternalId).build()));
    when(cityJSONRequestRepositoryMock.findByCommunityOwnerIdAndCriteria(
            eq(communityInternalId), any(), any(), eq(unpaged())))
        .thenReturn(List.of(finished, processing, failed));
    when(cityJSONRequestConverterMock.apply(any(), eq(additionalAttributes)))
        .thenReturn(exportedFile);

    subject.requestExport(
        communityOwnerId, exportId, from, to, additionalAttributes, List.of(FAILED));
    subject.requestExport(
        communityOwnerId, exportId, from, to, additionalAttributes, List.of(SUCCEEDED, FAILED));

    verify(cityJSONRequestConverterMock).apply(List.of(processing, failed), additionalAttributes);
    verify(cityJSONRequestConverterMock)
        .apply(List.of(finished, processing, failed), additionalAttributes);
    verify(cityJSONRequestRepositoryMock, never())
        .findSucceededByCommunityOwnerIdAndCriteria(any(), any(), any(), any());
  }

  @Test
  void request_export_throws_when_community_not_found() {
    when(communityAuthorizationRepositoryMock.findById("unknown")).thenReturn(Optional.empty());

    var actual =
        assertThrows(
            NoSuchElementException.class,
            () ->
                subject.requestExport(
                    "unknown", randomUUID().toString(), from, to, additionalAttributes, null));

    assertEquals("No value present", actual.getMessage());
    verify(bucketComponentMock, never()).upload(any(), any());
  }
}
