package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.detection.inference.InferenceModelCardConf;
import app.bpartners.geojobs.service.detection.inference.ModelCardMapping;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DetectionImageZoomResolverTest {
  ModelCardResolver modelCardResolverMock = mock();
  DetectionImageZoomResolver subject = new DetectionImageZoomResolver(modelCardResolverMock);

  @BeforeEach
  void setUp() {
    when(modelCardResolverMock.tilingZoomOf(TOITURE_REVETEMENT, null)).thenReturn(21);
    when(modelCardResolverMock.tilingZoomOf(USURE, null)).thenReturn(21);
    when(modelCardResolverMock.tilingZoomOf(ARBRE, null)).thenReturn(20);
    when(modelCardResolverMock.tilingZoomOf(PISCINE, null)).thenReturn(22);
    when(modelCardResolverMock.tilingZoomOf(PISCINE, 20.0)).thenReturn(18);
    when(modelCardResolverMock.tilingZoomOf(ARBRE, 20.0)).thenReturn(20);
  }

  @Test
  void defaults_to_zoom_20_without_any_object_configuration() {
    assertEquals(20, subject.apply(detection(true)));
  }

  @Test
  void uses_the_zoom_of_the_toiture_objects_for_a_toiture_detection() {
    assertEquals(21, subject.apply(detection(true, TOITURE_REVETEMENT, USURE)));
  }

  @Test
  void ignores_the_zoom_of_non_toiture_objects_requested_along_toiture() {
    assertEquals(21, subject.apply(detection(true, TOITURE_REVETEMENT, ARBRE)));
    assertEquals(21, subject.apply(detection(true, USURE, PISCINE)));
  }

  @Test
  void uses_the_highest_zoom_of_the_requested_objects_without_toiture() {
    assertEquals(22, subject.apply(detection(false, ARBRE, PISCINE)));
    assertEquals(20, subject.apply(detection(false, ARBRE)));
  }

  @Test
  void uses_the_zoom_mapped_for_the_spatial_resolution_of_the_detection() {
    var detection = detection(false, ARBRE, PISCINE);
    when(detection.spatialResolutionInCm()).thenReturn(20.0);

    assertEquals(20, subject.apply(detection));
  }

  @Test
  void toiture_zoom_is_the_highest_zoom_of_every_toiture_type() {
    assertEquals(21, subject.toitureZoom());
  }

  @Test
  void stays_on_zoom_20_for_every_detection_when_inference_api_is_disabled() {
    var conf = mock(InferenceModelCardConf.class);
    when(conf.getMappings())
        .thenReturn(
            List.of(
                new ModelCardMapping(TOITURE_REVETEMENT, "toiture", 21, 5.0),
                new ModelCardMapping(USURE, "toiture", 21, 5.0)));
    var disabledSubject =
        new DetectionImageZoomResolver(new ModelCardResolver(conf, mock(), false));

    assertEquals(20, disabledSubject.toitureZoom());
    assertEquals(20, disabledSubject.apply(detection(true, TOITURE_REVETEMENT, USURE)));
    assertEquals(20, disabledSubject.apply(detection(false, TOITURE_REVETEMENT)));
  }

  private Detection detection(boolean toiture, DetectableType... types) {
    var detection = mock(Detection.class);
    when(detection.hasRoofBasedModelName()).thenReturn(toiture);
    when(detection.spatialResolutionInCm()).thenReturn(null);
    when(detection.getDetectableObjectConfigurations())
        .thenReturn(
            Arrays.stream(types)
                .<DetectableObjectConfiguration>map(
                    type -> DetectableObjectConfiguration.builder().objectType(type).build())
                .toList());
    return detection;
  }
}
