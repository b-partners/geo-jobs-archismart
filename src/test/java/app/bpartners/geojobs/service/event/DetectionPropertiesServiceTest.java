package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.FeatureRoofResultPropertiesComputer;
import app.bpartners.geojobs.service.area.mutation.MutationContextFactory;
import app.bpartners.geojobs.service.area.mutation.model.AreaPictureHistoryResponse;
import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.geojson.GeometryCorrector;
import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

class DetectionPropertiesServiceTest {
  private final MutationContextFactory mutationContextFactoryMock = mock();

  @Test
  void vegetation_is_requested_when_a_vegetation_type_is_among_the_job_configurations() {
    var detection =
        Detection.builder()
            .detectableObjectConfigurations(
                List.of(configurationOf(TOITURE_REVETEMENT), configurationOf(ARBRE)))
            .build();

    assertTrue(DetectionPropertiesService.isVegetationRequested(detection));
  }

  @Test
  void vegetation_is_not_requested_for_a_roof_only_job() {
    var roofOnly =
        Detection.builder()
            .detectableObjectConfigurations(List.of(configurationOf(TOITURE_REVETEMENT)))
            .build();
    var noConfiguration = Detection.builder().build();

    assertFalse(DetectionPropertiesService.isVegetationRequested(roofOnly));
    assertFalse(DetectionPropertiesService.isVegetationRequested(noConfiguration));
  }

  @Test
  void apply_tells_the_computer_whether_the_job_requested_vegetation() {
    var computer = mock(FeatureRoofResultPropertiesComputer.class);
    var detectionRepository = mock(DetectionRepository.class);
    var geometryConverter = mock(GeometryConverter.class);
    var tileRepository = mock(MachineDetectedTileRepository.class);
    var subject =
        new DetectionPropertiesService(
            detectionRepository,
            computer,
            geometryConverter,
            tileRepository,
            mock(GeometryCorrector.class),
            mutationContextFactoryMock,
            false);
    var detection =
        Detection.builder()
            .id("detection-id")
            .zdjId("zdj-id")
            .detectableObjectModelList(List.of(new DetectableObjectModel().modelName(TOITURE)))
            .detectableObjectConfigurations(List.of(configurationOf(ARBRE)))
            .build();
    var roof = squareRestFeature();
    when(geometryConverter.apply(any())).thenReturn(squareMultiPolygon());
    when(tileRepository.findAllByZdjJobId("zdj-id")).thenReturn(List.of());
    when(computer.apply(any(), any(), any(), any(), any(), anyBoolean()))
        .thenReturn(new HashMap<>());
    when(detectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    subject.apply(detection, roof, List.of(roof));

    verify(computer).apply(eq(roof), any(), any(), any(), isNull(), eq(true));
  }

  private static Feature squareRestFeature() {
    var ring =
        List.of(
            List.of(BigDecimal.ZERO, BigDecimal.ZERO),
            List.of(BigDecimal.ONE, BigDecimal.ZERO),
            List.of(BigDecimal.ONE, BigDecimal.ONE),
            List.of(BigDecimal.ZERO, BigDecimal.ONE),
            List.of(BigDecimal.ZERO, BigDecimal.ZERO));
    var feature = new Feature();
    feature.setGeometry(
        new FeatureGeometry(
            new MultiPolygon().type(MULTI_POLYGON).coordinates(List.of(List.of(ring)))));
    feature.setProperties(new HashMap<>());
    return feature;
  }

  private static org.locationtech.jts.geom.MultiPolygon squareMultiPolygon() {
    var gf = new GeometryFactory();
    var square =
        gf.createPolygon(
            new Coordinate[] {
              new Coordinate(0, 0),
              new Coordinate(1, 0),
              new Coordinate(1, 1),
              new Coordinate(0, 1),
              new Coordinate(0, 0)
            });
    return gf.createMultiPolygon(new org.locationtech.jts.geom.Polygon[] {square});
  }

  private static DetectableObjectConfiguration configurationOf(DetectableType objectType) {
    return DetectableObjectConfiguration.builder().objectType(objectType).build();
  }

  private DetectionPropertiesService subjectWithMutation(boolean mutationEnabled) {
    return new DetectionPropertiesService(
        mock(DetectionRepository.class),
        mock(FeatureRoofResultPropertiesComputer.class),
        mock(GeometryConverter.class),
        mock(MachineDetectedTileRepository.class),
        mock(GeometryCorrector.class),
        mutationContextFactoryMock,
        mutationEnabled);
  }

  @Test
  void tryCreateMutationContext_returns_the_built_context_on_success_when_mutation_is_enabled() {
    var subject = subjectWithMutation(true);
    var detection = Detection.builder().id("detection-1").build();
    var roofGeometry = mock(Geometry.class);
    var expected =
        new MutationContext(
            new AreaPictureHistoryResponse.DatedImage(
                2022, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/old.jpg")),
            new AreaPictureHistoryResponse.DatedImage(
                2024, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/new.jpg")),
            new File("mask.png"));
    when(mutationContextFactoryMock.create(detection, roofGeometry)).thenReturn(expected);

    var actual = invokeTryCreateMutationContext(subject, detection, roofGeometry);

    assertEquals(expected, actual);
  }

  @Test
  void
      tryCreateMutationContext_returns_null_instead_of_propagating_when_factory_throws_and_mutation_is_enabled() {
    var subject = subjectWithMutation(true);
    var detection = Detection.builder().id("detection-1").build();
    var roofGeometry = mock(Geometry.class);
    when(mutationContextFactoryMock.create(detection, roofGeometry))
        .thenThrow(new IllegalStateException("geodata API unreachable"));

    var actual = invokeTryCreateMutationContext(subject, detection, roofGeometry);

    assertNull(actual);
  }

  @Test
  void tryCreateMutationContext_does_not_call_the_factory_when_mutation_is_disabled() {
    var subject = subjectWithMutation(false);
    var detection = Detection.builder().id("detection-1").build();
    var roofGeometry = mock(Geometry.class);

    var actual = invokeTryCreateMutationContext(subject, detection, roofGeometry);

    assertNull(actual);
    verifyNoInteractions(mutationContextFactoryMock);
  }

  @SneakyThrows
  private Object invokeTryCreateMutationContext(
      DetectionPropertiesService subject, Detection detection, Geometry roofGeometry) {
    Method method =
        DetectionPropertiesService.class.getDeclaredMethod(
            "tryCreateMutationContext", Detection.class, Geometry.class);
    method.setAccessible(true);
    return method.invoke(subject, detection, roofGeometry);
  }
}
