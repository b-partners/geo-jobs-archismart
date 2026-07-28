package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;
import static app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob.ZoomLevelEnum.HOUSES_0;
import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.*;
import static app.bpartners.geojobs.endpoint.rest.model.GeoJsonOutput.GEO_JSON;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.*;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.*;
import static app.bpartners.geojobs.file.hash.FileHashAlgorithm.SHA256;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_BUCKET_FOLDER;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.io.File.createTempFile;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionExcelFileSaved;
import app.bpartners.geojobs.endpoint.event.model.DetectionRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
import app.bpartners.geojobs.endpoint.event.model.DetectionTilingRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationJobVerificationSent;
import app.bpartners.geojobs.endpoint.event.model.zone.DetectionQualityControlFinished;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.*;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectionFromStatisticRestMapper;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectionFromStepMapper;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectionStepMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.DetectionFileObject;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.dashboard.AreaPictureApi;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureMapLayer;
import app.bpartners.geojobs.service.dashboard.component.Zoom;
import app.bpartners.geojobs.service.detection.*;
import app.bpartners.geojobs.service.detection.DetectionCreationMapper;
import app.bpartners.geojobs.service.event.FeatureVggRequestedService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.geoserver.GeoServerConfiguration;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import app.bpartners.geojobs.utils.FeatureCreator;
import app.bpartners.geojobs.utils.TaskStatisticCreator;
import app.bpartners.geojobs.utils.detection.DetectionCreator;
import app.bpartners.geojobs.utils.detection.ZoneDetectionJobCreator;
import app.bpartners.geojobs.validator.FeatureTypeChecker;
import app.bpartners.geojobs.validator.ZoneDetectionJobValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
class DetectionServiceTest {
  private static final String FEATURE_FILE_NAME_OK =
      "src"
          + File.separator
          + "test"
          + File.separator
          + "resources"
          + File.separator
          + "features"
          + File.separator
          + "features-ok.json";
  private static final String FEATURE_FILE_NAME_KO =
      "src"
          + File.separator
          + "test"
          + File.separator
          + "resources"
          + File.separator
          + "features"
          + File.separator
          + "features-ko.json";
  private static final String LATEST_DEFAULT_LAYER = "cite:PCRS";
  ZoneTilingJobService tilingJobServiceMock = mock();
  ZoneTilingJobMapper tilingJobMapperMock = mock();
  ZoneDetectionJobValidator detectionJobValidatorMock = mock();
  EventProducer eventProducerMock = mock();
  DetectionStepStatisticMapper stepStatisticMapper =
      new DetectionStepStatisticMapper(new StatusMapper<>());
  DetectionRepository detectionRepositoryMock = mock();
  CommunityUsedSurfaceService communityUsedSurfaceServiceMock = mock();
  BucketComponent bucketComponentMock = mock();
  GeoJsonConversionJobService conversionInitiationServiceMock = mock();
  DetectableObjectTypeMapper detectableObjectTypeMapper = new DetectableObjectTypeMapper();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  FeatureCreator featureCreator = new FeatureCreator();
  DetectionCreator detectionCreator = new DetectionCreator(featureCreator);
  AuthProvider authProviderMock = mock();
  GeoJsonConversionJobRepository geoJsonConversionJobRepositoryMock =
      mock(GeoJsonConversionJobRepository.class);
  DetectionGeoJsonUpdateValidator detectionGeoJsonUpdateValidator =
      new DetectionGeoJsonUpdateValidator();
  FeatureTypeChecker featureTypeChecker = new FeatureTypeChecker();
  ZoneDetectionJobCreator zoneDetectionJobCreator = new ZoneDetectionJobCreator();
  CommunityAuthorizationRepository communityAuthRepositoryMock = mock();
  TaskStatisticCreator taskStatisticCreator = new TaskStatisticCreator();
  DetectionFeaturesResultImageRetriever featureImageRetrieverMock =
      mock(DetectionFeaturesResultImageRetriever.class);
  DetectionImageAttributeRetriever imageAttributeRetrieverMock =
      mock(DetectionImageAttributeRetriever.class);
  DetectionVggAttributeRetriever vggAttributeRetrieverMock =
      mock(DetectionVggAttributeRetriever.class);
  RoofDelimiterMapper roofDelimiterMapper = mock();
  DetectionImageTileInfoOriginRetriever imageTileInfoOriginRetrieverMock = mock();
  DetectionFromStepMapper detectionFromStepMapper =
      new DetectionFromStepMapper(
          bucketComponentMock,
          featureImageRetrieverMock,
          imageAttributeRetrieverMock,
          vggAttributeRetrieverMock,
          new DetectionStepMapper(),
          roofDelimiterMapper,
          imageTileInfoOriginRetrieverMock);
  DetectionFromStatisticRestMapper detectionFromStatisticRestMapper =
      new DetectionFromStatisticRestMapper(detectionFromStepMapper, stepStatisticMapper);
  DetectionRestMapper detectionRestMapper =
      new DetectionRestMapper(detectionFromStatisticRestMapper);
  FeatureMapper featureMapperMock = mock();
  DetectionTilingStatisticsComputer detectionTilingStatisticsComputerMock =
      new DetectionTilingStatisticsComputer(tilingJobServiceMock, detectionFromStatisticRestMapper);
  DetectionTilingCreation detectionTilingCreationMock =
      new DetectionTilingCreation(
          tilingJobMapperMock,
          tilingJobServiceMock,
          detectionRepositoryMock,
          detectionTilingStatisticsComputerMock);
  DetectionMachineDetectionStatisticsComputer detectionMachineDetectionStatisticsComputerMock =
      new DetectionMachineDetectionStatisticsComputer(
          detectionFromStatisticRestMapper, zoneDetectionJobServiceMock);
  DetectionAddressConsumer detectionAddressConsumerMock = mock();
  GeometryConverter geometryConverterMock = mock();
  AreaPictureApi areaPictureApiMock = mock();
  SynchronousDetectionService synchronousDetectionServiceMock = mock();
  SynchronousDetectionValidator synchronousDetectionValidatorMock = mock();
  TileMultiPolygonFrame tileMultiPolygonFrameMock = mock();
  TileDuplicationRemover tileDuplicationRemoverMock = mock();
  MachineDetectionCreation machineDetectionCreationMock =
      new MachineDetectionCreation(
          zoneDetectionJobServiceMock,
          detectionJobValidatorMock,
          detectionMachineDetectionStatisticsComputerMock,
          tileDuplicationRemoverMock);
  private final String geoServerDummyUrl = "http://dummy";
  private final String e2ApiKey = randomUUID().toString();
  GeoServerConfiguration geoServerConfiguration = new GeoServerConfiguration(geoServerDummyUrl);
  DetectionSupportedAreaValidator detectionAreaValidatorMock = mock();
  DetectionStepMapper detectionStepMapper = new DetectionStepMapper();
  RoofAnalysisMailer roofAnalysisMailerMock = mock(RoofAnalysisMailer.class);
  FileWriter fileWriterMock = mock();
  DetectionRoofSlopeValidator detectionRoofSlopeValidatorMock = mock();
  BuildingFinder buildingFinderMock = mock();
  FeatureVggRequestedService featureVggRequestedServiceMock = mock();

  DetectionService subject =
      spy(
          new DetectionService(
              zoneDetectionJobServiceMock,
              tilingJobServiceMock,
              eventProducerMock,
              detectionRepositoryMock,
              communityUsedSurfaceServiceMock,
              bucketComponentMock,
              conversionInitiationServiceMock,
              new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false),
              authProviderMock,
              detectionGeoJsonUpdateValidator,
              communityAuthRepositoryMock,
              detectionTilingCreationMock,
              machineDetectionCreationMock,
              geoJsonConversionJobRepositoryMock,
              detectionAddressConsumerMock,
              synchronousDetectionServiceMock,
              synchronousDetectionValidatorMock,
              detectionStepMapper,
              roofAnalysisMailerMock,
              new DetectionCreationMapper(
                  detectableObjectTypeMapper,
                  featureTypeChecker,
                  communityAuthRepositoryMock,
                  areaPictureApiMock,
                  geoServerConfiguration,
                  geometryConverterMock,
                  buildingFinderMock),
              fileWriterMock,
              detectionRoofSlopeValidatorMock,
              featureVggRequestedServiceMock));

  @BeforeEach
  void setUp() {
    when(featureImageRetrieverMock.apply(any()))
        .thenAnswer(
            invocation ->
                ((app.bpartners.geojobs.repository.model.detection.Detection)
                        invocation.getArgument(0))
                    .getProvidedGeoJsonZone());
    when(communityAuthRepositoryMock.findByApiKey(any()))
        .thenReturn(
            Optional.of(CommunityAuthorization.builder().id(randomUUID().toString()).build()));
    when(communityAuthRepositoryMock.findById(any()))
        .thenReturn(Optional.of(CommunityAuthorization.builder().apiKey(e2ApiKey).build()));
    when(geoJsonConversionJobRepositoryMock.findByZoneDetectionJobId(any())).thenReturn(List.of());
    when(featureMapperMock.toDomainPolygon(any())).thenReturn(geometryFactory.createPolygon());
    when(featureMapperMock.toRest(any(), any(Integer.class), any()))
        .thenReturn(featureCreator.defaultFeatures().getFirst());
    when(imageTileInfoOriginRetrieverMock.apply(any())).thenReturn(null);

    var areaPictureMapLayerMock = mock(AreaPictureMapLayer.class);
    when(areaPictureMapLayerMock.name()).thenReturn(LATEST_DEFAULT_LAYER);
    when(areaPictureApiMock.getAreaPictureMapLayers(anyDouble(), anyDouble(), eq(e2ApiKey)))
        .thenReturn(List.of(areaPictureMapLayerMock));

    doNothing().when(detectionAreaValidatorMock).accept(any());
  }

  @Test
  void admin_role_process_request_accepted_when_all_data_ok() {
    var detectionIdentifier = randomUUID().toString();
    var createDetection =
        new CreateDetection()
            .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
            .geoServerProperties(null)
            .geoJsonZone(featureCreator.defaultFeatures());
    String communityOwnerId = randomUUID().toString();
    setUpAuthorityRoleProcessingMock(detectionIdentifier, null, ROLE_ADMIN);
    when(communityUsedSurfaceServiceMock.persistDetectionWithSurfaceUsage(any(), any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    var jtsMultiPolygonFrameMock = mock(MultiPolygon.class);
    when(jtsMultiPolygonFrameMock.contains(any())).thenReturn(true);
    var longitude = BigDecimal.valueOf(0);
    var latitude = BigDecimal.valueOf(1);
    when(geometryConverterMock.centroidFromGeometry(any()))
        .thenReturn(List.of(longitude, latitude));
    when(tileMultiPolygonFrameMock.apply(longitude, latitude))
        .thenReturn(Optional.of(jtsMultiPolygonFrameMock));
    when(communityAuthRepositoryMock.findById(any(String.class)))
        .thenReturn(
            Optional.<CommunityAuthorization>of(
                new CommunityAuthorization().builder().dashboardApiKey("apiKey").build()));
    when(areaPictureApiMock.getAreaPictureMapLayers(anyDouble(), anyDouble(), anyString()))
        .thenReturn(
            List.of(new AreaPictureMapLayer("id", LATEST_DEFAULT_LAYER, new Zoom("level", 24), 5)));

    var actual =
        detectionRestMapper.toRest(
            subject.processDetection(detectionIdentifier, createDetection, communityOwnerId, null));

    var expectedGeoServerProperties =
        geoServerConfiguration.defaultGeoServerProperties(LATEST_DEFAULT_LAYER, 5);
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(2)).accept(listCaptor.capture());
    var actualEvent = (DetectionTilingRequested) listCaptor.getAllValues().getLast().getFirst();
    assertEquals(REQUEST_ACCEPTED, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PROCESSING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
    assertEquals(expectedGeoServerProperties, actual.getGeoServerProperties());
    assertEquals(
        new DetectionTilingRequested(
            actualEvent.getDetectionIdentifier()), // internal detection no retrieved here
        actualEvent);
  }

  @Test
  void get_geo_json_conversion_when_machine_zdj_finished() {
    var detectionId = randomUUID().toString();
    var tilingJobId = randomUUID().toString();
    var machineDetectionJobId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingJobId, machineDetectionJobId);
    detection.setGeoServerProperties(new GeoServerProperties());
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    var createDetection = new CreateDetection().geoJsonZone(featureCreator.defaultFeatures());
    String communityOwnerId = null;
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    var zoneTilingJobMock = mock(ZoneTilingJob.class);
    var zoneDetectionJobMock =
        mock(app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.class);
    when(zoneTilingJobMock.isSucceeded()).thenReturn(true);
    when(tilingJobServiceMock.findById(detection.getZtjId())).thenReturn(zoneTilingJobMock);
    when(zoneDetectionJobServiceMock.findById(detection.getZdjId()))
        .thenReturn(zoneDetectionJobMock);
    when(zoneDetectionJobMock.isPending()).thenReturn(false);
    when(zoneTilingJobMock.isFinished()).thenReturn(true);
    when(zoneDetectionJobMock.isFinished()).thenReturn(true);
    when(zoneDetectionJobServiceMock.countInDoubtDetectedTileToDeliveryById(
            zoneTilingJobMock.getId()))
        .thenReturn(0L);
    var geoJsonConversionJobMock = mock(GeoJsonConversionJob.class);
    when(conversionInitiationServiceMock.getOrComputeGeoJsonConversionJob(
            detection, zoneDetectionJobMock))
        .thenReturn(geoJsonConversionJobMock);
    when(zoneDetectionJobServiceMock.computeTaskStatistics(any()))
        .thenReturn(
            TaskStatistic.builder()
                .taskStatusStatistics(List.of())
                .actualJobStatus(
                    JobStatus.builder()
                        .progression(FINISHED)
                        .health(app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED)
                        .build())
                .build());

    var actual =
        detectionRestMapper.toRest(
            subject.processDetection(detectionId, createDetection, communityOwnerId, null));

    verify(conversionInitiationServiceMock, only())
        .getOrComputeGeoJsonConversionJob(detection, zoneDetectionJobMock);
    assertEquals(MACHINE_DETECTION, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStep().getStatus().getHealth());
  }

  @Test
  void compute_annotation_verification_conversion_when_human_zdj_not_finished() {
    var detectionId = randomUUID().toString();
    var tilingJobId = randomUUID().toString();
    var humanZoneDetectionJobId = randomUUID().toString();
    var machineDetectionJobId = randomUUID().toString();

    var detection = detectionCreator.create(detectionId, tilingJobId, machineDetectionJobId);
    detection.setGeoServerProperties(new GeoServerProperties());
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    var createDetection = new CreateDetection().geoJsonZone(featureCreator.defaultFeatures());
    String communityOwnerId = null;
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    var zoneTilingJobMock = mock(ZoneTilingJob.class);
    var machineZoneDetectionJobMock =
        mock(app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.class);
    var humanZoneDetectionJob =
        mock(app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.class);

    when(zoneTilingJobMock.getId()).thenReturn(tilingJobId);
    when(machineZoneDetectionJobMock.getId()).thenReturn(machineDetectionJobId);
    when(humanZoneDetectionJob.getId()).thenReturn(humanZoneDetectionJobId);
    when(humanZoneDetectionJob.getDetectionType()).thenReturn(HUMAN);
    when(humanZoneDetectionJob.isSucceeded()).thenReturn(false);
    when(machineZoneDetectionJobMock.isPending()).thenReturn(false);
    when(zoneTilingJobMock.isFinished()).thenReturn(true);
    when(machineZoneDetectionJobMock.isFinished()).thenReturn(true);
    when(zoneTilingJobMock.isSucceeded()).thenReturn(true);
    when(tilingJobServiceMock.findById(detection.getZtjId())).thenReturn(zoneTilingJobMock);
    when(zoneDetectionJobServiceMock.findById(detection.getZdjId()))
        .thenReturn(machineZoneDetectionJobMock);
    when(zoneDetectionJobServiceMock.getByTilingJobId(zoneTilingJobMock.getId(), HUMAN))
        .thenReturn(humanZoneDetectionJob);
    when(zoneDetectionJobServiceMock.countInDoubtDetectedTileToDeliveryById(machineDetectionJobId))
        .thenReturn(1L);
    when(zoneDetectionJobServiceMock.computeTaskStatistics(any()))
        .thenReturn(
            TaskStatistic.builder()
                .taskStatusStatistics(List.of())
                .actualJobStatus(
                    JobStatus.builder()
                        .progression(FINISHED)
                        .health(app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED)
                        .build())
                .build());

    var actual =
        detectionRestMapper.toRest(
            subject.processDetection(detectionId, createDetection, communityOwnerId, null));

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(conversionInitiationServiceMock, never()).getOrComputeGeoJsonConversionJob(any(), any());
    verify(eventProducerMock, times(1)).accept(eventCaptor.capture());
    var annotationJobVerificationSent =
        (AnnotationJobVerificationSent) eventCaptor.getValue().getFirst();
    assertEquals(
        AnnotationJobVerificationSent.builder().humanZdjId(humanZoneDetectionJob.getId()).build(),
        annotationJobVerificationSent);
    assertEquals(MACHINE_DETECTION, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStep().getStatus().getHealth());
  }

  @Test
  void admin_role_can_process_tiling() {
    var detectionId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, null, null);
    detection.setGeoServerProperties(new GeoServerProperties());
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    var createDetection = new CreateDetection().geoJsonZone(featureCreator.defaultFeatures());
    String communityOwnerId = null;
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);

    var actual =
        detectionRestMapper.toRest(
            subject.processDetection(detectionId, createDetection, communityOwnerId, null));

    assertEquals(TILING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PENDING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  @Test
  void stuck_at_configuring_when_multipolygon_geojson_or_geoserver_properties_are_null() {
    var detectionId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, null, null);
    var createDetection = new CreateDetection().geoJsonZone(featureCreator.defaultFeatures());
    String communityOwnerId = null;
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);

    var actual =
        detectionRestMapper.toRest(
            subject.processDetection(detectionId, createDetection, communityOwnerId, null));

    assertEquals(REQUEST_ACCEPTED, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PENDING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  @Test
  void process_synchronously_returns_pipeline_result_when_no_error() {
    var detectionId = randomUUID().toString();
    String communityOwnerId = null;
    var existing =
        detectionCreator.create(detectionId, randomUUID().toString(), randomUUID().toString());
    var createDetection = new CreateDetection().geoJsonZone(featureCreator.defaultFeatures());
    when(synchronousDetectionValidatorMock.apply(any())).thenReturn(createDetection);
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(detectionId, communityOwnerId))
        .thenReturn(Optional.of(existing));
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(existing));

    var actual =
        subject.processDetectionSynchronously(
            detectionId, createDetection, communityOwnerId, false);

    verify(synchronousDetectionServiceMock).apply(any());
    assertEquals(existing.getId(), actual.getId());
    var computedStep = actual.getComputedStep();
    assertEquals(MACHINE_DETECTION, computedStep.getName());
    assertEquals(
        app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED,
        computedStep.getProgression());
    assertEquals(
        app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED, computedStep.getHealth());
  }

  @Test
  void process_synchronously_marks_failed_step_machine_detection_when_zdj_present() {
    var thrown =
        assertFailedStepPersistedOnPipelineError(
            detectionCreator.create(
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString()),
            MACHINE_DETECTION);
    assertEquals("boom", thrown.getMessage());
  }

  @Test
  void process_synchronously_marks_failed_step_tiling_when_only_ztj_present() {
    assertFailedStepPersistedOnPipelineError(
        detectionCreator.create(randomUUID().toString(), randomUUID().toString(), null), TILING);
  }

  @Test
  void process_synchronously_marks_failed_step_request_accepted_when_no_job_yet() {
    assertFailedStepPersistedOnPipelineError(
        detectionCreator.create(randomUUID().toString(), null, null), REQUEST_ACCEPTED);
  }

  private ApiException assertFailedStepPersistedOnPipelineError(
      app.bpartners.geojobs.repository.model.detection.Detection existing,
      app.bpartners.geojobs.endpoint.rest.model.DetectionStepName expectedFailedStepName) {
    var detectionId = existing.getId();
    String communityOwnerId = null;
    var createDetection = new CreateDetection().geoJsonZone(featureCreator.defaultFeatures());
    when(synchronousDetectionValidatorMock.apply(any())).thenReturn(createDetection);
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(detectionId, communityOwnerId))
        .thenReturn(Optional.of(existing));
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(existing));
    var pipelineError = new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, "boom");
    when(synchronousDetectionServiceMock.apply(any())).thenThrow(pipelineError);

    var thrown =
        assertThrows(
            ApiException.class,
            () ->
                subject.processDetectionSynchronously(
                    detectionId, createDetection, communityOwnerId, false));

    // the original error is rethrown unchanged so the synchronous caller still gets it
    assertEquals(pipelineError, thrown);
    var captor =
        ArgumentCaptor.forClass(app.bpartners.geojobs.repository.model.detection.Detection.class);
    verify(detectionRepositoryMock, atLeastOnce()).save(captor.capture());
    var persistedStep = captor.getAllValues().getLast().getStep();
    assertNotNull(persistedStep, "a terminal FAILED step must be persisted on pipeline error");
    assertEquals(expectedFailedStepName, persistedStep.getName());
    assertEquals(
        app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED,
        persistedStep.getProgression());
    assertEquals(
        app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED, persistedStep.getHealth());
    return thrown;
  }

  @Test
  void read_detection_ko() {
    var detectionId = "NonExistentDetectionId";
    setUpAuthorityRoleProcessingMock(null, null, ROLE_ADMIN);

    assertThrows(
        ApiException.class,
        () -> subject.getProcessedDetection(detectionId),
        "DetectionJob.id=NonExistentDetectionId is not found.");
  }

  @Test
  void read_detection_with_computed_step_ok() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, null);
    detection.setMultiPolygonGeoJsonZone(List.of());
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    var statistics = taskStatisticCreator.createProcessingTask(detection.getId(), DETECTION);
    statistics.setActualJobStatus(
        JobStatus.builder()
            .progression(PENDING)
            .health(app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN)
            .build());
    when(zoneDetectionJobServiceMock.computeTaskStatistics(any())).thenReturn(statistics);
    when(tilingJobServiceMock.computeTaskStatistics(any())).thenReturn(statistics);

    var actual = detectionRestMapper.toRest(subject.getProcessedDetection(detectionId));

    assertEquals(REQUEST_ACCEPTED, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PENDING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  @Test
  void read_detection_with_persisted_step_ok() {
    var detectionId = randomUUID().toString();
    var detection =
        app.bpartners.geojobs.repository.model.detection.Detection.builder()
            .id(detectionId)
            .endToEndId(detectionId)
            .detectionSteps(
                List.of(
                    app.bpartners.geojobs.repository.model.detection.DetectionStep.builder()
                        .name(POST_PROCESSING)
                        .progression(PROCESSING)
                        .health(app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN)
                        .message("persisted step message")
                        .creationDatetime(now())
                        .build()))
            .build();
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_INSURANCE);

    var actual = detectionRestMapper.toRest(subject.getProcessedDetection(detectionId));

    assertEquals(POST_PROCESSING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PROCESSING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
    assertEquals("persisted step message", actual.getStep().getStatus().getMessage());
  }

  @Test
  void rest_mapper_surfaces_job_message_on_computed_statistic_step() {
    var detection =
        detectionCreator.create(
            randomUUID().toString(), randomUUID().toString(), randomUUID().toString());
    var statistic = taskStatisticCreator.createProcessingTask(detection.getId(), DETECTION);
    statistic.setActualJobStatus(
        JobStatus.builder()
            .progression(PROCESSING)
            .health(app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED)
            .message("machine detection job failed: source timeout")
            .build());
    detection.setComputedStep(
        app.bpartners.geojobs.repository.model.detection.DetectionStep.builder()
            .name(MACHINE_DETECTION)
            .progression(PROCESSING)
            .health(app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED)
            .creationDatetime(now())
            .statistic(statistic)
            .build());

    var actual = detectionRestMapper.toRest(detection);

    assertEquals(MACHINE_DETECTION, actual.getStep().getName());
    assertEquals(
        "machine detection job failed: source timeout", actual.getStep().getStatus().getMessage());
  }

  @SneakyThrows
  @Test
  void read_detection_with_geo_json_file() {
    var detectionId = randomUUID().toString();
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(eq(detectionId), any()))
        .thenReturn(
            Optional.of(
                app.bpartners.geojobs.repository.model.detection.Detection.builder()
                    .geojsonS3FileKey("notNullKey")
                    .build()));
    when(bucketComponentMock.presign(any())).thenReturn("http://localhost");
    var principalMock = mock(Principal.class);
    when(principalMock.getPassword()).thenReturn("dummy");
    when(authProviderMock.getPrincipal()).thenReturn(principalMock);

    var actual = detectionRestMapper.toRest(subject.getProcessedDetection(detectionId));

    assertEquals(POST_PROCESSING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStep().getStatus().getHealth());
  }

  @Test
  void admin_role_read_detection_with_tiling_statistics() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, null);
    detection.setGeoServerProperties(new GeoServerProperties());
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);

    var actual = detectionRestMapper.toRest(subject.getProcessedDetection(detectionId));

    assertEquals(TILING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PENDING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  @Test
  void admin_role_read_detection_with_machine_detection_statistics() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detectionJobId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, detectionJobId);
    detection.setGeoServerProperties(new GeoServerProperties());
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    when(zoneDetectionJobServiceMock.findById(detectionJobId))
        .thenReturn(
            zoneDetectionJobCreator.create(
                detectionJobId,
                null,
                null,
                PENDING,
                app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN,
                new ZoneTilingJob()));

    var actual = detectionRestMapper.toRest(subject.getProcessedDetection(detectionId));

    assertEquals(MACHINE_DETECTION, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PENDING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  @Test
  void admin_role_read_detection_with_human_detection_statistics() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detectionJobId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, detectionJobId);
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    detection.setGeoServerProperties(new GeoServerProperties());
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    when(zoneDetectionJobServiceMock.countInDoubtDetectedTileToDeliveryById(detectionJobId))
        .thenReturn(1L);
    when(zoneDetectionJobServiceMock.findById(detectionJobId))
        .thenReturn(
            zoneDetectionJobCreator.create(
                detectionJobId,
                null,
                null,
                FINISHED,
                app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED,
                new ZoneTilingJob()));

    var actual = detectionRestMapper.toRest(subject.getProcessedDetection(detectionId));

    assertEquals(POST_PROCESSING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(FAILED, actual.getStep().getStatus().getHealth());
  }

  @Test
  void admin_role_read_detection_with_finished_machine_detection_statistics() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var zoneDetectionJobId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, zoneDetectionJobId);
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    detection.setGeoServerProperties(new GeoServerProperties());
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    when(zoneDetectionJobServiceMock.countInDoubtDetectedTileToDeliveryById(zoneDetectionJobId))
        .thenReturn(0L);
    when(zoneDetectionJobServiceMock.computeTaskStatistics(zoneDetectionJobId))
        .thenReturn(
            TaskStatistic.builder()
                .actualJobStatus(
                    JobStatus.builder()
                        .progression(FINISHED)
                        .health(app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED)
                        .build())
                .taskStatusStatistics(new ArrayList<>())
                .build());
    when(zoneDetectionJobServiceMock.findById(zoneDetectionJobId))
        .thenReturn(
            zoneDetectionJobCreator.create(
                zoneDetectionJobId,
                null,
                null,
                FINISHED,
                app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED,
                new ZoneTilingJob()));
    when(geoJsonConversionJobRepositoryMock.findByZoneDetectionJobId(zoneDetectionJobId))
        .thenReturn(
            List.of(
                GeoJsonConversionJob.builder()
                    .statusHistory(List.of(JobStatus.builder().creationDatetime(now()).build()))
                    .build()));

    var actual = detectionRestMapper.toRest(subject.getProcessedDetection(detectionId));

    assertEquals(MACHINE_DETECTION, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStep().getStatus().getHealth());
  }

  @Test
  void admin_role_read_detection_with_finished_failed_post_processing_statistics() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var zoneDetectionJobId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, zoneDetectionJobId);
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    detection.setGeoServerProperties(new GeoServerProperties());
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    when(zoneDetectionJobServiceMock.countInDoubtDetectedTileToDeliveryById(zoneDetectionJobId))
        .thenReturn(0L);
    when(zoneDetectionJobServiceMock.computeTaskStatistics(zoneDetectionJobId))
        .thenReturn(
            TaskStatistic.builder()
                .actualJobStatus(
                    JobStatus.builder()
                        .progression(FINISHED)
                        .health(app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED)
                        .build())
                .taskStatusStatistics(new ArrayList<>())
                .build());
    when(zoneDetectionJobServiceMock.findById(zoneDetectionJobId))
        .thenReturn(
            zoneDetectionJobCreator.create(
                zoneDetectionJobId,
                null,
                null,
                FINISHED,
                app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED,
                new ZoneTilingJob()));
    when(geoJsonConversionJobRepositoryMock.findByZoneDetectionJobId(zoneDetectionJobId))
        .thenReturn(List.of());

    var actual = detectionRestMapper.toRest(subject.getProcessedDetection(detectionId));

    assertEquals(POST_PROCESSING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(FAILED, actual.getStep().getStatus().getHealth());
  }

  @Test
  void admin_role_read_finished_geo_json_conversion_but_not_computed_geo_json_file_key() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detectionJobId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, detectionJobId);
    detection.setGeojsonS3FileKey(null); // Just to explicit it here
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    detection.setGeoServerProperties(new GeoServerProperties());
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    reset(geoJsonConversionJobRepositoryMock);
    when(geoJsonConversionJobRepositoryMock.findByZoneDetectionJobId(detectionJobId))
        .thenReturn(
            List.of(
                someGeoJsonConversionJob(
                    FINISHED,
                    app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED,
                    now())));
    when(zoneDetectionJobServiceMock.countInDoubtDetectedTileToDeliveryById(detectionJobId))
        .thenReturn(0L);
    when(zoneDetectionJobServiceMock.computeTaskStatistics(detectionJobId))
        .thenReturn(
            TaskStatistic.builder()
                .actualJobStatus(
                    JobStatus.builder()
                        .progression(FINISHED)
                        .health(app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED)
                        .build())
                .taskStatusStatistics(new ArrayList<>())
                .build());
    when(zoneDetectionJobServiceMock.findById(detectionJobId))
        .thenReturn(
            zoneDetectionJobCreator.create(
                detectionJobId,
                null,
                null,
                FINISHED,
                app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED,
                new ZoneTilingJob()));

    var actual = detectionRestMapper.toRest(subject.getProcessedDetection(detectionId));

    assertEquals(POST_PROCESSING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PROCESSING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  @Test
  void admin_role_read_finished_geo_json_conversion_with_computed_geo_json_file_key() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detectionJobId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, detectionJobId);
    detection.setGeojsonS3FileKey(randomUUID().toString()); // Just to explicit it here
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    detection.setGeoServerProperties(new GeoServerProperties());
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    reset(geoJsonConversionJobRepositoryMock);
    when(geoJsonConversionJobRepositoryMock.findByZoneDetectionJobId(detectionJobId))
        .thenReturn(
            List.of(
                someGeoJsonConversionJob(
                    FINISHED,
                    app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED,
                    now())));
    when(zoneDetectionJobServiceMock.countInDoubtDetectedTileToDeliveryById(detectionJobId))
        .thenReturn(0L);
    when(zoneDetectionJobServiceMock.computeTaskStatistics(detectionJobId))
        .thenReturn(
            TaskStatistic.builder()
                .actualJobStatus(
                    JobStatus.builder()
                        .progression(FINISHED)
                        .health(app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED)
                        .build())
                .taskStatusStatistics(new ArrayList<>())
                .build());
    when(zoneDetectionJobServiceMock.findById(detectionJobId))
        .thenReturn(
            zoneDetectionJobCreator.create(
                detectionJobId,
                null,
                null,
                FINISHED,
                app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED,
                new ZoneTilingJob()));

    var actual = detectionRestMapper.toRest(subject.getProcessedDetection(detectionId));

    assertEquals(POST_PROCESSING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStep().getStatus().getHealth());
  }

  @Test
  void admin_role_read_pending_geo_json_conversion_statistics() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detectionJobId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, detectionJobId);
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    detection.setGeoServerProperties(new GeoServerProperties());
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    reset(geoJsonConversionJobRepositoryMock);
    when(geoJsonConversionJobRepositoryMock.findByZoneDetectionJobId(detectionJobId))
        .thenReturn(
            List.of(
                someGeoJsonConversionJob(
                    PROCESSING, app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN, now()),
                someGeoJsonConversionJob(
                    PENDING,
                    app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN,
                    now())) // latest is chosen
            );
    when(zoneDetectionJobServiceMock.countInDoubtDetectedTileToDeliveryById(detectionJobId))
        .thenReturn(0L);
    when(zoneDetectionJobServiceMock.computeTaskStatistics(detectionJobId))
        .thenReturn(
            TaskStatistic.builder()
                .actualJobStatus(
                    JobStatus.builder()
                        .progression(FINISHED)
                        .health(app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED)
                        .build())
                .taskStatusStatistics(new ArrayList<>())
                .build());
    when(zoneDetectionJobServiceMock.findById(detectionJobId))
        .thenReturn(
            zoneDetectionJobCreator.create(
                detectionJobId,
                null,
                null,
                FINISHED,
                app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED,
                new ZoneTilingJob()));

    var actual = detectionRestMapper.toRest(subject.getProcessedDetection(detectionId));

    assertEquals(POST_PROCESSING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PENDING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  private GeoJsonConversionJob someGeoJsonConversionJob(
      app.bpartners.geojobs.job.model.Status.ProgressionStatus progressionStatus,
      app.bpartners.geojobs.job.model.Status.HealthStatus healthStatus,
      Instant submissionInstant) {
    return GeoJsonConversionJob.builder()
        .statusHistory(
            List.of(
                JobStatus.builder().progression(progressionStatus).health(healthStatus).build()))
        .submissionInstant(submissionInstant)
        .build();
  }

  @Test
  void admin_role_read_processing_geo_json_conversion_statistics() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detectionJobId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, detectionJobId);
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    detection.setGeoServerProperties(new GeoServerProperties());
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    reset(geoJsonConversionJobRepositoryMock);
    when(geoJsonConversionJobRepositoryMock.findByZoneDetectionJobId(detectionJobId))
        .thenReturn(
            List.of(
                someGeoJsonConversionJob(
                    PENDING, app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN, now()),
                someGeoJsonConversionJob(
                    PENDING, app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN, now()),
                someGeoJsonConversionJob(
                    FINISHED,
                    app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED,
                    now()), // only succeeded is considered
                someGeoJsonConversionJob(
                    PROCESSING,
                    app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN,
                    now())) // latest is chosen
            );
    when(zoneDetectionJobServiceMock.countInDoubtDetectedTileToDeliveryById(detectionJobId))
        .thenReturn(0L);
    when(zoneDetectionJobServiceMock.computeTaskStatistics(detectionJobId))
        .thenReturn(
            TaskStatistic.builder()
                .actualJobStatus(
                    JobStatus.builder()
                        .progression(FINISHED)
                        .health(app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED)
                        .build())
                .taskStatusStatistics(new ArrayList<>())
                .build());
    when(zoneDetectionJobServiceMock.findById(detectionJobId))
        .thenReturn(
            zoneDetectionJobCreator.create(
                detectionJobId,
                null,
                null,
                FINISHED,
                app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED,
                new ZoneTilingJob()));

    var actual = detectionRestMapper.toRest(subject.getProcessedDetection(detectionId));

    assertEquals(POST_PROCESSING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PROCESSING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  @SneakyThrows
  @Test
  void admin_role_read_detection_with_generated_geo_json() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detectionJobId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, detectionJobId);
    var geoJsonS3FileKey = "https://dummyGeoJsonFileKey.com";
    detection.setGeojsonS3FileKey(geoJsonS3FileKey);
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    when(zoneDetectionJobServiceMock.findById(detectionJobId))
        .thenReturn(
            zoneDetectionJobCreator.create(
                detectionJobId,
                null,
                null,
                FINISHED,
                app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED,
                new ZoneTilingJob()));
    when(bucketComponentMock.presign(any())).thenReturn(geoJsonS3FileKey);

    var actual = detectionRestMapper.toRest(subject.getProcessedDetection(detectionId));

    assertEquals(POST_PROCESSING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStep().getStatus().getHealth());
  }

  private void setUpAuthorityRoleProcessingMock(
      String detectionId,
      app.bpartners.geojobs.repository.model.detection.Detection detection,
      Authority.Role authorityRole) {
    if (detectionId != null) {
      if (detection != null) {
        when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(eq(detectionId), any()))
            .thenReturn(Optional.of(detection));
        when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));
      } else {
        when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.empty());
        when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(
                eq(detectionId), any(String.class)))
            .thenReturn(Optional.empty());
      }
    }
    when(authProviderMock.getPrincipal())
        .thenReturn(new Principal("mockApiKey", Set.of(new Authority(authorityRole))));
    when(tilingJobMapperMock.from(any()))
        .thenReturn(new CreateZoneTilingJob().geoServerUrl("http://localhost").zoomLevel(HOUSES_0));
    when(tilingJobMapperMock.toDomain(any(), any())).thenReturn(new ZoneTilingJob());
    when(tilingJobServiceMock.create(any(), any())).thenReturn(new ZoneTilingJob());
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(tilingJobServiceMock.computeTaskStatistics(any()))
        .thenReturn(somePendingTaskStatistic(GeoJobType.TILING));
    when(zoneDetectionJobServiceMock.computeTaskStatistics(any()))
        .thenReturn(somePendingTaskStatistic(DETECTION));
  }

  private static TaskStatistic somePendingTaskStatistic(GeoJobType geoJobType) {
    return TaskStatistic.builder()
        .actualJobStatus(
            JobStatus.builder()
                .progression(PENDING)
                .health(app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN)
                .creationDatetime(now())
                .build())
        .updatedAt(now())
        .taskStatusStatistics(new ArrayList<>())
        .jobType(geoJobType)
        .build();
  }

  private static TaskStatistic someFinishedTaskStatistic(GeoJobType geoJobType) {
    return TaskStatistic.builder()
        .actualJobStatus(
            JobStatus.builder()
                .progression(FINISHED)
                .health(app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED)
                .creationDatetime(now())
                .build())
        .updatedAt(now())
        .taskStatusStatistics(new ArrayList<>())
        .jobType(geoJobType)
        .build();
  }

  @Test
  void configure_addresses_ok() {
    var detectionId = randomUUID().toString();
    var detection =
        detectionCreator.create(
            detectionId, randomUUID().toString(), randomUUID().toString(), null);
    var addresses = List.of("11-7 Rue Mot, 94120 Fontenay-sous-Bois, France");
    var expected =
        detection.toBuilder()
            .detectableObjectModelList(List.of())
            .convertedAddresses(addresses)
            .build();
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(any(), any()))
        .thenReturn(Optional.of(detection));
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    var principalMock = mock(Principal.class);
    when(principalMock.getPassword()).thenReturn("dummy");
    when(authProviderMock.getPrincipal()).thenReturn(principalMock);

    var actual =
        detectionRestMapper.toRest(subject.configureDetectionAddresses(detectionId, addresses));
    var expectedRestDetection =
        new Detection()
            .id(detectionId)
            .addresses(addresses)
            .shapeUrl(null)
            .excelUrl(null)
            .geoJsonZone(null)
            .geoServerProperties(detection.getGeoServerProperties())
            .detectableObjectModel(detection.getDetectableObjectModel())
            .detectableObjectModelList(detection.getDetectableObjectModelList())
            .step(
                new DetectionStep()
                    .name(REQUEST_ACCEPTED)
                    .status(
                        new Status()
                            .progression(Status.ProgressionEnum.PROCESSING)
                            .health(UNKNOWN)
                            .creationDatetime(actual.getStep().getStatus().getCreationDatetime()))
                    .statistics(List.of())
                    .updatedAt(actual.getStep().getUpdatedAt()))
            .geoJsonOutput(GEO_JSON);
    verify(detectionAddressConsumerMock, only()).accept(expected);
    assertEquals(expectedRestDetection, actual);
  }

  @SneakyThrows
  @Test
  void configure_shape_file_ok() {
    var shapeFile = createTempFile(randomUUID().toString(), randomUUID().toString());
    var detection =
        detectionCreator.create(
            randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), null);
    var detectionE2eId = detection.getEndToEndId();
    var shapeFileBucketKey = "detections/shape/" + detectionE2eId;
    var shapeUrl = "https://localhost";
    when(bucketComponentMock.upload(shapeFile, shapeFileBucketKey))
        .thenReturn(new FileHash(SHA256, "dummy"));
    when(bucketComponentMock.presign(shapeFileBucketKey)).thenReturn(shapeUrl);
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(eq(detectionE2eId), any()))
        .thenReturn(Optional.of(detection));
    var principalMock = mock(Principal.class);
    when(principalMock.getPassword()).thenReturn("dummy");
    when(authProviderMock.getPrincipal()).thenReturn(principalMock);

    var actual = detectionRestMapper.toRest(subject.configureShapeFile(detectionE2eId, shapeFile));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, only()).accept(listCaptor.capture());
    var detectionSaved = (DetectionSaved) listCaptor.getValue().getFirst();
    var expectedSavedDetection = detection.toBuilder().shapeFileKey(shapeFileBucketKey).build();
    var expectedDetectionSavedEvent =
        DetectionSaved.builder().detectionIdentifier(expectedSavedDetection.getId()).build();
    var expectedRestDetection =
        new Detection()
            .id(detectionE2eId)
            .shapeUrl(shapeUrl)
            .excelUrl(null)
            .geoJsonZone(null)
            .geoServerProperties(detection.getGeoServerProperties())
            .detectableObjectModel(detection.getDetectableObjectModel())
            .detectableObjectModelList(detection.getDetectableObjectModelList())
            .step(
                new DetectionStep()
                    .name(REQUEST_ACCEPTED)
                    .status(
                        new Status()
                            .progression(Status.ProgressionEnum.PENDING)
                            .health(UNKNOWN)
                            .creationDatetime(actual.getStep().getStatus().getCreationDatetime()))
                    .statistics(List.of())
                    .updatedAt(actual.getStep().getUpdatedAt()))
            .geoJsonOutput(GEO_JSON);
    assertEquals(expectedDetectionSavedEvent, detectionSaved);
    assertEquals(expectedRestDetection, actual);
  }

  @Test
  @SneakyThrows
  void configure_excel_file_ok() {
    var excelFile = createTempFile(randomUUID().toString(), randomUUID().toString());
    var detection =
        detectionCreator.create(
            randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), List.of());
    var detectionE2eId = detection.getEndToEndId();
    var excelFileBucketKey = "detections/excel/" + detectionE2eId + ".xlsx";
    var excelUrl = "https://localhost";
    when(bucketComponentMock.upload(excelFile, excelFileBucketKey))
        .thenReturn(new FileHash(SHA256, "dummy"));
    when(bucketComponentMock.presign(excelFileBucketKey)).thenReturn(excelUrl);
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(eq(detectionE2eId), any()))
        .thenReturn(Optional.of(detection));
    var principalMock = mock(Principal.class);
    when(principalMock.getPassword()).thenReturn("dummy");
    when(authProviderMock.getPrincipal()).thenReturn(principalMock);

    var actual = detectionRestMapper.toRest(subject.configureExcelFile(detectionE2eId, excelFile));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(2)).accept(listCaptor.capture());
    var detectionSaved = (DetectionSaved) listCaptor.getAllValues().getLast().getFirst();
    var detectionExcelFileSaved =
        (DetectionExcelFileSaved) listCaptor.getAllValues().getFirst().getFirst();
    var expectedSavedDetection = detection.toBuilder().excelFileKey(excelFileBucketKey).build();
    var expectedDetectionSavedEvent =
        DetectionSaved.builder().detectionIdentifier(expectedSavedDetection.getId()).build();
    var expectedDetectionExcelFileSaved =
        DetectionExcelFileSaved.builder().detection(expectedSavedDetection).build();
    var expectedRestDetection =
        new Detection()
            .id(detectionE2eId)
            .excelUrl(excelUrl)
            .shapeUrl(null)
            .geoJsonZone(detection.getProvidedGeoJsonZone())
            .geoServerProperties(detection.getGeoServerProperties())
            .detectableObjectModel(detection.getDetectableObjectModel())
            .detectableObjectModelList(detection.getDetectableObjectModelList())
            .step(
                new DetectionStep()
                    .name(REQUEST_ACCEPTED)
                    .status(
                        new Status()
                            .progression(Status.ProgressionEnum.PENDING)
                            .health(UNKNOWN)
                            .creationDatetime(actual.getStep().getStatus().getCreationDatetime()))
                    .statistics(List.of())
                    .updatedAt(actual.getStep().getUpdatedAt()))
            .geoJsonOutput(GEO_JSON);
    assertEquals(expectedDetectionSavedEvent, detectionSaved);
    assertEquals(expectedRestDetection, actual);
    assertEquals(expectedDetectionExcelFileSaved, detectionExcelFileSaved);
  }

  @Test
  void finalize_geo_json_configuring_ko() {
    var featuresFile = new File(FEATURE_FILE_NAME_KO);
    var detection =
        detectionCreator.create(
            randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), null);
    var detectionId = detection.getId();
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));

    var actual =
        assertThrows(
            ApiException.class, () -> subject.finalizeGeoJsonConfig(detectionId, featuresFile));

    assertTrue(
        actual.getMessage().contains("Unable to convert uploaded file to Features, exception="));
  }

  @SneakyThrows
  @Test
  void finalize_geo_json_configuring_ok() {
    var featuresFile = new File(FEATURE_FILE_NAME_OK);
    var detection =
        detectionCreator.create(
            randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), List.of());
    var detectionId = detection.getId();
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));

    var actual =
        detectionRestMapper.toRest(subject.finalizeGeoJsonConfig(detectionId, featuresFile));

    var detectionCaptor =
        ArgumentCaptor.forClass(app.bpartners.geojobs.repository.model.detection.Detection.class);
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(detectionRepositoryMock).save(detectionCaptor.capture());
    verify(eventProducerMock, only()).accept(listCaptor.capture());
    var savedDetection = detectionCaptor.getValue();
    var detectionProvided = (DetectionSaved) listCaptor.getValue().getFirst();
    var expectedDetectionSaved =
        detection.toBuilder()
            .providedGeoJsonZone(
                featureCreator.defaultFeatures().stream()
                    .map(FeatureMapper::toDomainFeature)
                    .toList())
            .build();
    var expectedRestDetection =
        new Detection()
            .id(detectionId)
            .geoJsonZone(featureCreator.defaultFeatures())
            .geoServerProperties(detection.getGeoServerProperties())
            .detectableObjectModel(detection.getDetectableObjectModel())
            .detectableObjectModelList(detection.getDetectableObjectModelList())
            .step(
                new DetectionStep()
                    .name(REQUEST_ACCEPTED)
                    .status(
                        new Status()
                            .progression(Status.ProgressionEnum.PROCESSING)
                            .health(UNKNOWN)
                            .creationDatetime(actual.getStep().getStatus().getCreationDatetime()))
                    .statistics(List.of())
                    .updatedAt(actual.getStep().getUpdatedAt()))
            .geoJsonOutput(GEO_JSON);
    assertEquals(
        DetectionSaved.builder().detectionIdentifier(detectionId).build(), detectionProvided);
    assertEquals(expectedDetectionSaved, savedDetection);
    assertEquals(expectedRestDetection, actual);
  }

  @Test
  void configure_detection_file_result() throws IOException {
    var detectionId = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var multiPartFileMock = mock(MultipartFile.class);
    File fileMock = mock(File.class);
    byte[] bytes = new byte[] {};

    var principalMock = mock(Principal.class);
    when(principalMock.getPassword()).thenReturn(randomUUID().toString());
    when(authProviderMock.getPrincipal()).thenReturn(principalMock);
    var detection =
        new app.bpartners.geojobs.repository.model.detection.Detection()
            .toBuilder()
                .id(detectionId)
                .emailReceiver("random@gmail.com")
                .endToEndId(communityOwnerId)
                .detectionSteps(
                    List.of(
                        app.bpartners.geojobs.repository.model.detection.DetectionStep.builder()
                            .name(POST_PROCESSING)
                            .progression(FINISHED)
                            .health(app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED)
                            .build()))
                .build();
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(any(), any()))
        .thenReturn(Optional.of(detection));
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(multiPartFileMock.getBytes()).thenReturn(bytes);
    when(fileWriterMock.apply(bytes, null)).thenReturn(fileMock);
    var actual =
        detectionRestMapper.toRest(
            subject.configureFileResult(
                communityOwnerId, detectionId, multiPartFileMock, "geojson"));

    var stringCaptor = ArgumentCaptor.forClass(String.class);
    var listCaptor = ArgumentCaptor.forClass(List.class);
    assertEquals(POST_PROCESSING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStep().getStatus().getHealth());
    verify(bucketComponentMock, times(1)).upload(eq(fileMock), stringCaptor.capture());
    verify(detectionRepositoryMock).save(any());
    verify(eventProducerMock, times(2)).accept(listCaptor.capture());
    var detectionQualityControlFinished =
        (DetectionQualityControlFinished)
            listCaptor.getAllValues().stream()
                .flatMap(List::stream)
                .filter(DetectionQualityControlFinished.class::isInstance)
                .findFirst()
                .orElseThrow();
    assertEquals(new DetectionQualityControlFinished(detection), detectionQualityControlFinished);
    assertEquals(EVENT_STACK_2, detectionQualityControlFinished.getEventStack());
    assertEquals(Duration.ofSeconds(30L), detectionQualityControlFinished.maxConsumerDuration());
    assertEquals(
        Duration.ofSeconds(30L),
        detectionQualityControlFinished.maxConsumerBackoffBetweenRetries());
    assertTrue(stringCaptor.getValue().contains(GEO_JSON_BUCKET_FOLDER));
  }

  @SneakyThrows
  @Test
  void unable_to_update_geo_json() {
    var featuresFile = new File(FEATURE_FILE_NAME_OK);
    var shapeFile = createTempFile(randomUUID().toString(), randomUUID().toString());
    var excelFile = createTempFile(randomUUID().toString(), randomUUID().toString());
    var detection1 =
        detectionCreator.create(
            randomUUID().toString(),
            randomUUID().toString(),
            randomUUID().toString(),
            featureCreator.defaultFeatures());
    var detection2 =
        detectionCreator
            .create(
                randomUUID().toString(),
                randomUUID().toString(),
                randomUUID().toString(),
                List.of())
            .toBuilder()
            .shapeFileKey("notNullShapeFileKey")
            .build();
    var detection3 =
        detectionCreator
            .create(
                randomUUID().toString(),
                randomUUID().toString(),
                randomUUID().toString(),
                List.of())
            .toBuilder()
            .excelFileKey("notNullExcelFileKey")
            .build();
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findById(detection1.getId())).thenReturn(Optional.of(detection1));
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(
            eq(detection2.getEndToEndId()), any()))
        .thenReturn(Optional.of(detection2));
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(
            eq(detection3.getEndToEndId()), any()))
        .thenReturn(Optional.of(detection3));
    var principalMock = mock(Principal.class);
    when(principalMock.getPassword()).thenReturn("dummy");
    when(authProviderMock.getPrincipal()).thenReturn(principalMock);

    var actual1 =
        assertThrows(
            BadRequestException.class,
            () -> subject.finalizeGeoJsonConfig(detection1.getId(), featuresFile));
    var actual2 =
        assertThrows(
            BadRequestException.class,
            () -> subject.configureExcelFile(detection2.getEndToEndId(), excelFile));
    var actual3 =
        assertThrows(
            BadRequestException.class,
            () -> subject.configureShapeFile(detection3.getEndToEndId(), shapeFile));

    assertEquals(
        "Unable to finalize Detection(id="
            + detection1.getId()
            + ") geoJson as it already has values",
        actual1.getMessage());
    assertEquals(
        "Unable to configure Detection(id="
            + detection2.getId()
            + ") geoJson as it is already being configuring",
        actual2.getMessage());
    assertEquals(
        "Unable to configure Detection(id="
            + detection3.getId()
            + ") geoJson as it is already being configuring",
        actual3.getMessage());
  }

  @Test
  void update_detection_step() {
    var detectionId = randomUUID().toString();
    var detectionEntity =
        detectionCreator.create(detectionId, randomUUID().toString(), randomUUID().toString());
    var restStep =
        new DetectionStep()
            .name(MACHINE_DETECTION)
            .status(new Status().progression(Status.ProgressionEnum.FINISHED).health(SUCCEEDED));
    var principalMock = mock(Principal.class);
    var communityAuthorizationMock = mock(CommunityAuthorization.class);
    var apiKey = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();

    when(communityAuthorizationMock.getId()).thenReturn(communityOwnerId);
    when(communityAuthRepositoryMock.findByApiKey(apiKey))
        .thenReturn(Optional.of(communityAuthorizationMock));
    when(principalMock.getApiKey()).thenReturn(apiKey);
    when(authProviderMock.getPrincipal()).thenReturn(principalMock);
    when(detectionRepositoryMock.existsById(any(String.class))).thenReturn(true);
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(
            eq(detectionId), eq(communityOwnerId)))
        .thenReturn(Optional.of(detectionEntity));
    var actual =
        detectionRestMapper.toRest(
            subject.updateDetectionStep(detectionId, communityOwnerId, restStep));

    verify(detectionRepositoryMock, times(1))
        .findByEndToEndIdAndCommunityOwnerId(eq(detectionId), any());
    assertEquals(MACHINE_DETECTION, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStep().getStatus().getHealth());
  }

  @Test
  void throw_not_found_during_get_detection_file_object() {
    var detectionE2Id = randomUUID().toString();
    when(detectionRepositoryMock.findByEndToEndId(detectionE2Id)).thenReturn(Optional.empty());

    var actual =
        assertThrows(NotFoundException.class, () -> subject.getDetectionFileObjects(detectionE2Id));

    assertEquals("Detection.id= " + detectionE2Id + " not found.", actual.getMessage());
  }

  @Test
  void get_detection_file_object_when_detection_found() {
    var detectionE2Id = randomUUID().toString();
    var detectionMock = mock(app.bpartners.geojobs.repository.model.detection.Detection.class);
    var detectionFileObjectMock = mock(DetectionFileObject.class);

    when(detectionMock.getFileObjects()).thenReturn(List.of(detectionFileObjectMock));
    when(detectionRepositoryMock.findByEndToEndId(detectionE2Id))
        .thenReturn(Optional.of(detectionMock));

    var actual = subject.getDetectionFileObjects(detectionE2Id);

    assertEquals(List.of(detectionFileObjectMock), actual);
  }

  @Test
  void compute_roofs_properties_when_not_containing_slope_and_height_property() {
    var detectionE2Id = randomUUID().toString();
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = mock(app.bpartners.geojobs.repository.model.detection.Detection.class);
    var featureWithDelimitationMock = mock(FeatureWithDelimitation.class);
    var featureMock = mock(Feature.class);
    var domainResultMock = mock(app.bpartners.geojobs.repository.model.detection.Detection.class);

    when(featureMock.getProperties()).thenReturn(new HashMap<>());
    when(featureWithDelimitationMock.delimitations()).thenReturn(List.of(featureMock));
    when(detectionRepositoryMock.findByEndToEndId(detectionE2Id))
        .thenReturn(Optional.of(detectionMock));
    when(detectionMock.getId()).thenReturn(detectionIdentifier);
    when(detectionMock.getEndToEndId()).thenReturn(detectionE2Id);
    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(List.of(featureWithDelimitationMock));
    doNothing().when(detectionRoofSlopeValidatorMock).accept(detectionMock);
    doReturn(domainResultMock).when(subject).getProcessedDetection(detectionE2Id);

    var actual = subject.computeRoofsProperties(detectionE2Id);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var detectionRoofSlopeEvent =
        (DetectionRoofSlopeAndHeightRequested) listCaptor.getValue().getFirst();
    assertEquals(
        DetectionRoofSlopeAndHeightRequested.builder().detectionId(detectionIdentifier).build(),
        detectionRoofSlopeEvent);
    assertEquals(domainResultMock, actual);
  }

  @Test
  void do_not_compute_roofs_properties_when_already_containing_slope_and_height_property() {
    var detectionE2Id = randomUUID().toString();
    var detectionMock = mock(app.bpartners.geojobs.repository.model.detection.Detection.class);
    var featureWithDelimitationMock = mock(FeatureWithDelimitation.class);
    var domainResultMock = mock(app.bpartners.geojobs.repository.model.detection.Detection.class);

    when(detectionMock.getRoofPropertiesComputationCreationDatetime()).thenReturn(Instant.now());
    when(detectionMock.getEndToEndId()).thenReturn(detectionE2Id);
    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(List.of(featureWithDelimitationMock));
    when(detectionRepositoryMock.findByEndToEndId(detectionE2Id))
        .thenReturn(Optional.of(detectionMock));
    doNothing().when(detectionRoofSlopeValidatorMock).accept(detectionMock);
    doReturn(domainResultMock).when(subject).getProcessedDetection(detectionE2Id);

    var actual = subject.computeRoofsProperties(detectionE2Id);

    verify(eventProducerMock, never()).accept(any());

    assertEquals(domainResultMock, actual);
  }

  @Test
  void throw_not_found_when_detection_not_found() {
    var detectionE2Id = randomUUID().toString();
    when(detectionRepositoryMock.findByEndToEndId(detectionE2Id)).thenReturn(Optional.empty());

    var actual =
        assertThrows(NotFoundException.class, () -> subject.computeRoofsProperties(detectionE2Id));

    var expectedExceptionMessage = "Detection.e2Id " + detectionE2Id + " not found.";
    assertEquals(expectedExceptionMessage, actual.getMessage());
  }

  @Test
  void produces_vgg_computing_throws_not_found_when_detection_absent() {
    var detectionId = randomUUID().toString();
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.empty());

    var actual =
        assertThrows(NotFoundException.class, () -> subject.producesVggComputing(detectionId));

    assertEquals("Detection.id=" + detectionId + " not found", actual.getMessage());
    verify(featureVggRequestedServiceMock, never()).apply(any(), any());
    verify(eventProducerMock, never()).accept(any());
  }

  @Test
  void produces_vgg_computing_throws_bad_request_when_vgg_already_computed() {
    var detectionId = randomUUID().toString();
    var detection =
        app.bpartners.geojobs.repository.model.detection.Detection.builder()
            .id(detectionId)
            .vggFileKey("detections/vgg/" + detectionId + ".json")
            .build();
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));

    var actual =
        assertThrows(BadRequestException.class, () -> subject.producesVggComputing(detectionId));

    assertEquals(
        "Detection.id=" + detectionId + " already has computed its VGG", actual.getMessage());
    verify(featureVggRequestedServiceMock, never()).apply(any(), any());
    verify(eventProducerMock, never()).accept(any());
  }

  @Test
  void produces_vgg_computing_throws_bad_request_when_no_image_output_needed() {
    var detectionId = randomUUID().toString();
    var detection =
        app.bpartners.geojobs.repository.model.detection.Detection.builder()
            .id(detectionId)
            .vggFileKey(null)
            .needsImageOutput(false)
            .build();
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));

    var actual =
        assertThrows(BadRequestException.class, () -> subject.producesVggComputing(detectionId));

    assertEquals(
        "Detection.id=" + detectionId + " does not need image output so can not produce VGG",
        actual.getMessage());
    verify(featureVggRequestedServiceMock, never()).apply(any(), any());
    verify(eventProducerMock, never()).accept(any());
  }

  @Test
  void produces_vgg_computing_applies_synchronously_when_single_provided_feature() {
    var detectionId = randomUUID().toString();
    var providedFeature = featureCreator.defaultFeatures().getFirst();
    var detection =
        app.bpartners.geojobs.repository.model.detection.Detection.builder()
            .id(detectionId)
            .endToEndId(detectionId)
            .needsImageOutput(true)
            .providedGeoJsonZone(List.of(FeatureMapper.toDomainFeature(providedFeature)))
            .build();
    var detectionWithVgg =
        app.bpartners.geojobs.repository.model.detection.Detection.builder()
            .id(detectionId)
            .endToEndId(detectionId)
            .build();
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));
    when(featureVggRequestedServiceMock.apply(eq(detection), any())).thenReturn(detectionWithVgg);

    var actual = subject.producesVggComputing(detectionId);

    verify(featureVggRequestedServiceMock).apply(eq(detection), any());
    verify(eventProducerMock, never()).accept(any());
    // no persisted step, no zdj nor ztj: falls back on a REQUEST_ACCEPTED computed statistic step
    assertEquals(REQUEST_ACCEPTED, actual.getComputedStep().getName());
  }

  @Test
  void produces_vgg_computing_produces_events_when_multiple_provided_features() {
    var detectionId = randomUUID().toString();
    var providedFeature = featureCreator.defaultFeatures().getFirst();
    var detection =
        app.bpartners.geojobs.repository.model.detection.Detection.builder()
            .id(detectionId)
            .endToEndId(detectionId)
            .needsImageOutput(true)
            .providedGeoJsonZone(
                List.of(
                    FeatureMapper.toDomainFeature(providedFeature),
                    FeatureMapper.toDomainFeature(providedFeature)))
            .build();
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));

    var actual = subject.producesVggComputing(detectionId);

    verify(featureVggRequestedServiceMock, never()).apply(any(), any());
    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(2)).accept(eventCaptor.capture());
    eventCaptor.getAllValues().stream()
        .flatMap(List::stream)
        .forEach(
            event -> {
              assertInstanceOf(FeatureVggRequested.class, event);
              assertEquals(detectionId, ((FeatureVggRequested) event).getDetectionIdentifier());
            });
    assertEquals(REQUEST_ACCEPTED, actual.getComputedStep().getName());
  }
}
