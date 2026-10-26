package app.bpartners.geojobs.service.coverage;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.CLIMAT_RESILIENCE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.REVETEMENT_OBSTACLE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.STATIONNEMENT;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TAMPONS;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.CHEMINEE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE_CLAIR;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE_INTENSE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.LUMINEUX;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE_CLAIR;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE_COULEUR;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE_NOIRCIE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.OBSTACLE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PANNEAU_PHOTOVOLTAIQUE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PLACE_STANDARD;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE_IMPORTANTE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE_LEGER;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.VELUX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.service.detection.inference.InferenceModelCardConf;
import app.bpartners.geojobs.service.detection.inference.ModelCardMapping;
import app.bpartners.geojobs.service.detection.inference.ModelCardRegistry;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupportedModelResolverTest {
  InferenceModelCardConf conf = mock();
  SupportedModelResolver subject =
      new SupportedModelResolver(
          new DetectableObjectTypeMapper(),
          new ModelCardResolver(conf, mock(ModelCardRegistry.class)));

  @BeforeEach
  void setUp() {
    var mappings = new ArrayList<ModelCardMapping>();
    mappings.add(new ModelCardMapping(TOITURE_REVETEMENT, "bp-toiture-revetements-v2.0", 21, 5.0));
    mappings.add(new ModelCardMapping(TOITURE_REVETEMENT, "revets-fp-v1", 19, 20.0));
    mappings.add(new ModelCardMapping(PANNEAU_PHOTOVOLTAIQUE, "test_pl_obs", 21, 5.0));
    mappings.add(new ModelCardMapping(PANNEAU_PHOTOVOLTAIQUE, "obstacles-fp-v1", 19, 20.0));
    for (var type : List.of(OBSTACLE, CHEMINEE, VELUX)) {
      mappings.add(new ModelCardMapping(type, "obstacles-only-ignore-index-c4", 21, 5.0));
      mappings.add(new ModelCardMapping(type, "obstacles-fp-v1", 19, 20.0));
    }
    mappings.add(new ModelCardMapping(LUMINEUX, "obstacles-fp-v1", 19, 20.0));
    for (var type :
        List.<DetectableType>of(
            MOISISSURE_NOIRCIE,
            MOISISSURE_CLAIR,
            MOISISSURE_COULEUR,
            MOISISSURE,
            USURE_IMPORTANTE,
            USURE_LEGER,
            USURE,
            HUMIDITE_INTENSE,
            HUMIDITE_CLAIR,
            HUMIDITE)) {
      mappings.add(new ModelCardMapping(type, "damages-ignore-index-c4", 21, 5.0));
    }
    mappings.add(new ModelCardMapping(PLACE_STANDARD, "stationnement-obb-2cls-v0", 20, 5.0));
    when(conf.getMappings()).thenReturn(mappings);
  }

  @Test
  void toiture_is_supported_at_5_cm() {
    assertEquals(true, subject.supportedModels(5).contains(TOITURE));
  }

  @Test
  void toiture_is_not_supported_at_20_cm_as_damages_have_no_20_cm_model_card() {
    assertEquals(false, subject.supportedModels(20).contains(TOITURE));
  }

  @Test
  void revetement_obstacle_is_supported_at_5_and_20_cm_as_lumineux_only_applies_at_20_cm() {
    assertEquals(true, subject.supportedModels(5).contains(REVETEMENT_OBSTACLE));
    assertEquals(true, subject.supportedModels(20).contains(REVETEMENT_OBSTACLE));
  }

  @Test
  void types_without_model_card_are_only_supported_at_5_cm() {
    assertEquals(true, subject.supportedModels(5).contains(CLIMAT_RESILIENCE));
    assertEquals(false, subject.supportedModels(20).contains(CLIMAT_RESILIENCE));
    assertEquals(true, subject.supportedModels(5).contains(STATIONNEMENT));
    assertEquals(false, subject.supportedModels(20).contains(STATIONNEMENT));
  }

  @Test
  void model_without_object_type_is_never_supported() {
    assertEquals(false, subject.supportedModels(5).contains(TAMPONS));
  }

  @Test
  void unmapped_precision_supports_no_model() {
    assertEquals(List.of(), subject.supportedModels(10));
  }

  @Test
  void unknown_precision_supports_no_model() {
    assertEquals(List.of(), subject.supportedModels(null));
  }
}
