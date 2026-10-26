package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.BUILDING;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_1;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.OBSTACLE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerProperties;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.inference.InferenceModelCardConf;
import app.bpartners.geojobs.service.detection.inference.ModelCardMapping;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
import org.junit.jupiter.api.Test;

class DetectionTilingCreationTest {
  ZoneTilingJobMapper zoneTilingJobMapperMock = mock();
  ZoneTilingJobService zoneTilingJobServiceMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  InferenceModelCardConf conf = mock();

  @Test
  void tiles_on_zoom_20_only_when_inference_api_is_disabled_whatever_the_mapping() {
    givenMappingOnZoom21AndTilingJobStubs();
    var subject = tilingCreationWith(new ModelCardResolver(conf, mock(), false));

    subject.processTiling(toitureDetection());

    verify(zoneTilingJobServiceMock).getTilingTasks(any(), any(), eq(List.of(HOUSES_0)));
  }

  @Test
  void tiles_on_the_mapped_zoom_when_inference_api_is_enabled() {
    givenMappingOnZoom21AndTilingJobStubs();
    var subject = tilingCreationWith(new ModelCardResolver(conf, mock(), true));

    subject.processTiling(toitureDetection());

    verify(zoneTilingJobServiceMock).getTilingTasks(any(), any(), eq(List.of(HOUSES_1)));
  }

  @Test
  void tiles_on_the_zoom_mapped_for_the_precision_of_the_detection_layer() {
    when(conf.getMappings())
        .thenReturn(
            List.of(
                new ModelCardMapping(OBSTACLE, "obstacles-5cm", 20, 5.0),
                new ModelCardMapping(OBSTACLE, "obstacles-20cm", 19, 20.0)));
    stubTilingJob();
    var subject = tilingCreationWith(new ModelCardResolver(conf, mock(), true));

    subject.processTiling(obstacleDetection(20));
    subject.processTiling(obstacleDetection(5));

    verify(zoneTilingJobServiceMock).getTilingTasks(any(), any(), eq(List.of(BUILDING)));
    verify(zoneTilingJobServiceMock).getTilingTasks(any(), any(), eq(List.of(HOUSES_0)));
  }

  private DetectionTilingCreation tilingCreationWith(ModelCardResolver resolver) {
    return new DetectionTilingCreation(
        zoneTilingJobMapperMock,
        zoneTilingJobServiceMock,
        detectionRepositoryMock,
        mock(DetectionTilingStatisticsComputer.class),
        resolver);
  }

  private void givenMappingOnZoom21AndTilingJobStubs() {
    when(conf.getMappings())
        .thenReturn(List.of(new ModelCardMapping(TOITURE_REVETEMENT, "toiture", 21, 5.0)));
    stubTilingJob();
  }

  private void stubTilingJob() {
    var job = ZoneTilingJob.builder().id(randomUUID().toString()).build();
    when(zoneTilingJobMapperMock.from(any(Detection.class))).thenReturn(new CreateZoneTilingJob());
    when(zoneTilingJobMapperMock.toDomain(any(), any())).thenReturn(job);
    when(zoneTilingJobServiceMock.create(any(), any())).thenReturn(job);
    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  private Detection obstacleDetection(int precisionLevelInCm) {
    return Detection.builder()
        .id(randomUUID().toString())
        .geoServerProperties(
            new GeoServerProperties()
                .geoServerParameter(
                    new GeoServerParameter().precisionLevelInCm(precisionLevelInCm)))
        .detectableObjectConfigurations(
            List.of(DetectableObjectConfiguration.builder().objectType(OBSTACLE).build()))
        .build();
  }

  private Detection toitureDetection() {
    return Detection.builder()
        .id(randomUUID().toString())
        .detectableObjectConfigurations(
            List.of(DetectableObjectConfiguration.builder().objectType(TOITURE_REVETEMENT).build()))
        .build();
  }
}
