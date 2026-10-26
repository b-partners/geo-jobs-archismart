package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.FeatureRoofResultPropertiesComputer;
import app.bpartners.geojobs.service.area.mutation.MutationContextFactory;
import app.bpartners.geojobs.service.area.mutation.model.AreaPictureHistoryResponse;
import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.geojson.GeometryCorrector;
import java.io.File;
import java.lang.reflect.Method;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

class DetectionPropertiesServiceTest {
  private final MutationContextFactory mutationContextFactoryMock = mock();

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
