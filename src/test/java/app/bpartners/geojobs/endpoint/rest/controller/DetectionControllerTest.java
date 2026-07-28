package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.REQUEST_ACCEPTED;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.CreateDetectionMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionFileObjectMapper;
import app.bpartners.geojobs.endpoint.rest.controller.v1.DetectionController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectionRestMapper;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectionSurfaceUnitMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.DetectionAuthorizer;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.endpoint.rest.validator.CreateDetectionExportRequestValidator;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.MediaTypeGuesser;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.CommunityUsedSurfaceService;
import app.bpartners.geojobs.service.DetectionExportService;
import app.bpartners.geojobs.service.DetectionService;
import app.bpartners.geojobs.validator.ConfigureAddressValidator;
import app.bpartners.geojobs.validator.CreateDetectionValidator;
import app.bpartners.geojobs.validator.GetUsageValidator;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DetectionControllerTest {
  DetectionExportService detectionExportServiceMock = mock();
  CreateDetectionExportRequestValidator createDetectionExportRequestValidatorMock = mock();
  CommunityUsedSurfaceService communityUsedSurfaceServiceMock = mock();
  GetUsageValidator getUsageValidatorMock = mock();
  CommunityAuthorizationRepository communityAuthRepositoryMock = mock();
  AuthProvider authProviderMock = mock();
  DetectionSurfaceUnitMapper surfaceUnitMapper = new DetectionSurfaceUnitMapper();
  FileWriter fileWriterMock = mock();
  MediaTypeGuesser mediaTypeGuesserMock = mock();
  ConfigureAddressValidator configureAddressValidatorMock = mock();
  DetectionAuthorizer detectionAuthorizerMock = mock();
  CreateDetectionValidator createDetectionValidatorMock = mock(CreateDetectionValidator.class);
  DetectionService detectionServiceMock = mock();
  DetectionFileObjectMapper detectionFileObjectMapperMock = mock();
  CreateDetectionMapper createDetectionMapperMock = mock();
  DetectionRestMapper detectionRestMapperMock = mock();
  DetectionController subject =
      new DetectionController(
          detectionExportServiceMock,
          createDetectionExportRequestValidatorMock,
          communityUsedSurfaceServiceMock,
          getUsageValidatorMock,
          communityAuthRepositoryMock,
          authProviderMock,
          surfaceUnitMapper,
          detectionAuthorizerMock,
          fileWriterMock,
          mediaTypeGuesserMock,
          configureAddressValidatorMock,
          createDetectionValidatorMock,
          detectionServiceMock,
          detectionFileObjectMapperMock,
          createDetectionMapperMock,
          detectionRestMapperMock);

  @BeforeEach
  void setup() {
    when(authProviderMock.getPrincipal())
        .thenReturn(new Principal("dummyApiKey", Set.of(new Authority(ROLE_ADMIN))));
    when(communityAuthRepositoryMock.findByApiKey(any())).thenReturn(Optional.empty());
    doNothing().when(createDetectionValidatorMock).accept(any());
  }

  @Test
  void export_detections_ok() {
    var communityId = "community_id";
    var exportId = randomUUID().toString();
    var from = LocalDate.of(2024, 1, 1);
    var to = LocalDate.of(2024, 1, 31);
    var additionalAttributes =
        List.of(DetectionExportAttribute.AREA, DetectionExportAttribute.ZONE_NAME);
    var presignUrl = "https://bucket/presigned-url";
    var createDetectionExportRequest =
        new CreateDetectionExportRequest()
            .from(from)
            .to(to)
            .additionalExportedAttributes(additionalAttributes);
    when(detectionExportServiceMock.requestDetectionExport(
            communityId, exportId, from, to, additionalAttributes))
        .thenReturn(presignUrl);

    var actual = subject.exportDetections(communityId, exportId, createDetectionExportRequest);

    var expected =
        new DetectionExportRequest()
            .id(exportId)
            .from(from)
            .to(to)
            .additionalExportedAttributes(additionalAttributes)
            .fileUrl(presignUrl);
    assertEquals(expected, actual);
    verify(detectionExportServiceMock)
        .requestDetectionExport(communityId, exportId, from, to, additionalAttributes);
  }

  @Test
  void get_file_objects() {
    var detectionE2Id = randomUUID().toString();
    var detectionFileObjectMock =
        mock(app.bpartners.geojobs.repository.model.detection.DetectionFileObject.class);
    var restDetectionFileObject = mock(DetectionFileObject.class);
    when(detectionServiceMock.getDetectionFileObjects(detectionE2Id))
        .thenReturn(List.of(detectionFileObjectMock));
    when(detectionFileObjectMapperMock.toRest(detectionFileObjectMock))
        .thenReturn(restDetectionFileObject);

    var actual = subject.getDetectionFileObjects(detectionE2Id);

    assertEquals(List.of(restDetectionFileObject), actual);
  }

  @Test
  void compute_roof_slope() {
    var detectionIdentifier = randomUUID().toString();
    var domainDetectionMock =
        mock(app.bpartners.geojobs.repository.model.detection.Detection.class);
    var restDetectionMock = mock(Detection.class);
    when(detectionServiceMock.computeRoofsProperties(detectionIdentifier))
        .thenReturn(domainDetectionMock);
    when(detectionRestMapperMock.toRest(domainDetectionMock)).thenReturn(restDetectionMock);

    var actual = subject.computeDetectionRoofsProperties(detectionIdentifier);

    assertEquals(restDetectionMock, actual);
  }

  @Test
  void community_get_usage_ok() {
    var detectionUsageOk = mock(DetectionUsage.class);
    when(communityUsedSurfaceServiceMock.getUsage(any(), any())).thenReturn(detectionUsageOk);
    doNothing().when(getUsageValidatorMock).accept(any());

    var actual = subject.getDetectionUsage(DetectionSurfaceUnit.SQUARE_METER);

    assertEquals(detectionUsageOk, actual);
  }

  @Test
  void processDetectionSynchronously_ok() {
    var detectionId = randomUUID().toString();
    var createDetectionDebugModeMock = mock(CreateDetectionDebugMode.class);
    var createDetection = mock(CreateDetection.class);
    var principal = mock(Principal.class);
    var communityAuth = mock(CommunityAuthorization.class);
    var domainDetectionMock =
        mock(app.bpartners.geojobs.repository.model.detection.Detection.class);
    var expectedDetection = mock(Detection.class);
    when(authProviderMock.getPrincipal()).thenReturn(principal);
    when(principal.getPassword()).thenReturn("api-key");
    when(communityAuthRepositoryMock.findByApiKey("api-key"))
        .thenReturn(Optional.of(communityAuth));
    when(communityAuth.getId()).thenReturn("community-id");
    doNothing().when(detectionAuthorizerMock).accept(detectionId, createDetection, principal);
    when(detectionServiceMock.processDetectionSynchronously(anyString(), any(), anyString(), any()))
        .thenReturn(domainDetectionMock);
    when(detectionRestMapperMock.toRest(domainDetectionMock)).thenReturn(expectedDetection);
    when(createDetectionMapperMock.fromDebugMode(any())).thenReturn(createDetection);

    var actual = subject.processDetectionSynchronously(detectionId, createDetectionDebugModeMock);

    assertEquals(expectedDetection, actual);
    verify(detectionServiceMock)
        .processDetectionSynchronously(detectionId, createDetection, "community-id", false);
    verify(detectionAuthorizerMock).accept(detectionId, createDetection, principal);
    verify(authProviderMock, times(2)).getPrincipal();
    verify(principal).getPassword();
    verify(communityAuthRepositoryMock).findByApiKey("api-key");
    verify(communityAuth).getId();
  }

  @Test
  void computeDetectionVgg_ok() {
    var detectionIdentifier = randomUUID().toString();
    var domainDetectionMock =
        mock(app.bpartners.geojobs.repository.model.detection.Detection.class);
    var restDetectionMock = mock(Detection.class);
    when(detectionServiceMock.producesVggComputing(detectionIdentifier))
        .thenReturn(domainDetectionMock);
    when(detectionRestMapperMock.toRest(domainDetectionMock)).thenReturn(restDetectionMock);

    var actual = subject.computeDetectionVgg(detectionIdentifier);

    assertEquals(restDetectionMock, actual);
    verify(detectionServiceMock).producesVggComputing(detectionIdentifier);
  }

  @Test
  void configureRoofDelimiter_ok() {
    var actualException =
        assertThrows(
            NotImplementedException.class,
            () -> subject.configureDetectionRoofDelimiter("detectionId", new RoofDelimiter()));

    assertEquals(
        "POST /detections/{id}/roofDelimiter not supported anymore.", actualException.getMessage());
  }

  @Test
  void updateDetectionStep_ok() {
    var domainDetectionMock =
        mock(app.bpartners.geojobs.repository.model.detection.Detection.class);
    var restDetectionMock = mock(Detection.class);
    when(detectionServiceMock.updateDetectionStep(
            anyString(), anyString(), any(DetectionStep.class)))
        .thenReturn(domainDetectionMock);
    when(detectionRestMapperMock.toRest(domainDetectionMock)).thenReturn(restDetectionMock);

    Detection actual =
        subject.updateCommunityDetectionStep(
            randomUUID().toString(),
            randomUUID().toString(),
            new DetectionStep().name(REQUEST_ACCEPTED));

    assertNotNull(actual);
    assertEquals(restDetectionMock, actual);
  }

  @Test
  void configureDetectionAddresses_ok() {
    var addresses = List.of(new Address().address("dummyAddress"));
    var addressesStrings = addresses.stream().map(Address::getAddress).toList();
    var domainDetectionMock =
        mock(app.bpartners.geojobs.repository.model.detection.Detection.class);
    when(detectionServiceMock.configureDetectionAddresses(anyString(), any()))
        .thenReturn(domainDetectionMock);
    when(detectionRestMapperMock.toRest(domainDetectionMock))
        .thenReturn(new Detection().addresses(addressesStrings));

    Detection actual = subject.configureDetectionAddresses("detectionId", addresses);

    assertNotNull(actual);
    assertEquals(addressesStrings, actual.getAddresses());

    verify(detectionServiceMock).configureDetectionAddresses("detectionId", addressesStrings);
  }
}
