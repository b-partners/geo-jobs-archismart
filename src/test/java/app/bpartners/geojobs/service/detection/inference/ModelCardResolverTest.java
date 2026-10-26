package app.bpartners.geojobs.service.detection.inference;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.CHEMINEE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ESPACE_VERT;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.OBSTACLE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PANNEAU_PHOTOVOLTAIQUE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOMBE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.VELUX;
import static app.bpartners.geojobs.service.detection.inference.DeployedModelCard.ProblemType.OBJECT_DETECTION;
import static app.bpartners.geojobs.service.detection.inference.DeployedModelCard.ProblemType.SEMANTIC_SEGMENTATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModelCardResolverTest {
  private static final Predicate<DeployedModelCard> ANY = modelCard -> true;
  private static final int DEFAULT_ZOOM = 20;
  private static final int DAMAGES_ZOOM = 21;
  private static final DeployedModelCard DAMAGES =
      new DeployedModelCard("damages", SEMANTIC_SEGMENTATION, 1, true, 1024, 1024, true);
  private static final DeployedModelCard VEGETATION =
      new DeployedModelCard("vegetation", SEMANTIC_SEGMENTATION, 1, true, 1024, 1024, true);
  private static final DeployedModelCard TOMBS =
      new DeployedModelCard("tombs", OBJECT_DETECTION, 1, false, null, null, null);
  private static final ResolvedModelCard RESOLVED_DAMAGES =
      new ResolvedModelCard(DAMAGES, DAMAGES_ZOOM, 5.0);
  private static final ResolvedModelCard RESOLVED_TOMBS =
      new ResolvedModelCard(TOMBS, DEFAULT_ZOOM, 10.0);
  private static final ResolvedModelCard RESOLVED_VEGETATION =
      new ResolvedModelCard(VEGETATION, DEFAULT_ZOOM, 10.0);

  InferenceModelCardConf conf = mock();
  ModelCardRegistry registry = mock();
  ModelCardResolver subject = new ModelCardResolver(conf, registry);

  @BeforeEach
  void setUp() {
    when(conf.getMappings())
        .thenReturn(
            List.of(
                new ModelCardMapping(HUMIDITE, "damages", DAMAGES_ZOOM, 5.0),
                new ModelCardMapping(USURE, "damages", DAMAGES_ZOOM, 5.0),
                new ModelCardMapping(TOMBE, "tombs", DEFAULT_ZOOM, 10.0),
                new ModelCardMapping(ESPACE_VERT, "vegetation", DEFAULT_ZOOM, 10.0),
                new ModelCardMapping(ARBRE, "vegetation", DEFAULT_ZOOM, 10.0),
                new ModelCardMapping(PISCINE, "missing", DAMAGES_ZOOM, 5.0)));
    when(registry.findById("damages")).thenReturn(Optional.of(DAMAGES));
    when(registry.findById("tombs")).thenReturn(Optional.of(TOMBS));
    when(registry.findById("vegetation")).thenReturn(Optional.of(VEGETATION));
    when(registry.findById("missing")).thenReturn(Optional.empty());
  }

  @Test
  void types_sharing_a_model_card_resolve_to_it_once_with_its_zoom_and_resolution() {
    var resolution = subject.resolve(configs(HUMIDITE, USURE), null, DAMAGES_ZOOM, ANY);

    assertEquals(List.of(RESOLVED_DAMAGES), resolution.modelCards());
    assertTrue(resolution.unmappedTypes().isEmpty());
  }

  @Test
  void resolves_only_the_model_cards_of_the_tile_zoom() {
    var configs = configs(TOMBE, HUMIDITE, USURE);

    assertEquals(
        List.of(RESOLVED_TOMBS), subject.resolve(configs, null, DEFAULT_ZOOM, ANY).modelCards());
    assertEquals(
        List.of(RESOLVED_DAMAGES), subject.resolve(configs, null, DAMAGES_ZOOM, ANY).modelCards());
  }

  @Test
  void vegetation_types_resolve_to_their_model_card_without_any_flag() {
    var resolution =
        subject.resolve(configs(HUMIDITE, ESPACE_VERT, ARBRE), null, DEFAULT_ZOOM, ANY);

    assertEquals(List.of(RESOLVED_VEGETATION), resolution.modelCards());
    assertTrue(resolution.unmappedTypes().isEmpty());
  }

  @Test
  void reports_unmapped_types_only_on_the_default_zoom() {
    var configs = configs(HUMIDITE, CHEMINEE);

    assertEquals(
        Set.of(CHEMINEE), subject.resolve(configs, null, DEFAULT_ZOOM, ANY).unmappedTypes());
    assertTrue(subject.resolve(configs, null, DAMAGES_ZOOM, ANY).unmappedTypes().isEmpty());
  }

  @Test
  void incompatible_model_cards_are_reported_as_unsupported_types() {
    var resolution =
        subject.resolve(
            configs(HUMIDITE, USURE, TOMBE),
            null,
            DAMAGES_ZOOM,
            modelCard -> !modelCard.modelCardId().equals("damages"));

    assertTrue(resolution.modelCards().isEmpty());
    assertEquals(Set.of(HUMIDITE, USURE), resolution.unsupportedTypes());
    assertTrue(resolution.unmappedTypes().isEmpty());
  }

  @Test
  void throws_when_mapped_model_card_is_not_deployed() {
    assertThrows(
        IllegalStateException.class,
        () -> subject.resolve(configs(PISCINE), null, DAMAGES_ZOOM, ANY));
  }

  @Test
  void tiling_zoom_levels_are_distinct_sorted_and_default_for_unmapped_types() {
    assertEquals(
        List.of(20, 21), subject.tilingZoomLevels(configs(HUMIDITE, TOMBE, CHEMINEE), null));
    assertEquals(List.of(21), subject.tilingZoomLevels(configs(HUMIDITE, USURE), null));
    assertEquals(List.of(DEFAULT_ZOOM), subject.tilingZoomLevels(configs(CHEMINEE), null));
    assertEquals(List.of(DEFAULT_ZOOM), subject.tilingZoomLevels(List.of(), null));
  }

  @Test
  void supported_spatial_resolutions_are_shared_by_every_type_and_default_to_5_cm_when_unmapped() {
    assertEquals(Set.of(5.0), subject.supportedSpatialResolutionsInCm(configs(HUMIDITE, USURE)));
    assertEquals(Set.of(), subject.supportedSpatialResolutionsInCm(configs(HUMIDITE, TOMBE)));
    assertEquals(Set.of(5.0), subject.supportedSpatialResolutionsInCm(configs(CHEMINEE)));
    assertEquals(Set.of(), subject.supportedSpatialResolutionsInCm(configs(TOMBE, CHEMINEE)));
    assertEquals(Set.of(5.0), subject.supportedSpatialResolutionsInCm(List.of()));
  }

  @Test
  void ignores_every_mapping_and_stays_on_zoom_20_and_5_cm_when_inference_api_is_disabled() {
    var disabled = new ModelCardResolver(conf, registry, false);
    var configs = configs(HUMIDITE, USURE, TOMBE, CHEMINEE);

    assertEquals(DEFAULT_ZOOM, disabled.tilingZoomOf(HUMIDITE, null));
    assertEquals(List.of(DEFAULT_ZOOM), disabled.tilingZoomLevels(configs, null));
    assertEquals(Set.of(5.0), disabled.supportedSpatialResolutionsInCm(configs));
    var resolution = disabled.resolve(configs, null, DEFAULT_ZOOM, ANY);
    assertTrue(resolution.modelCards().isEmpty());
    assertTrue(resolution.unsupportedTypes().isEmpty());
    assertEquals(Set.of(HUMIDITE, USURE, TOMBE, CHEMINEE), resolution.unmappedTypes());
    assertTrue(disabled.resolve(configs, null, DAMAGES_ZOOM, ANY).unmappedTypes().isEmpty());
    verifyNoInteractions(registry);
  }

  @Test
  void does_not_even_read_the_conf_when_inference_api_is_disabled() {
    var untouchedConf = mock(InferenceModelCardConf.class);
    var disabled = new ModelCardResolver(untouchedConf, registry, false);

    disabled.tilingZoomLevels(configs(HUMIDITE), null);
    disabled.resolve(configs(HUMIDITE), null, DEFAULT_ZOOM, ANY);

    verifyNoInteractions(untouchedConf);
    assertFalse(disabled.isInferenceApiEnabled());
  }

  @Test
  void tiling_zoom_of_a_type() {
    assertEquals(DAMAGES_ZOOM, subject.tilingZoomOf(HUMIDITE, null));
    assertEquals(DEFAULT_ZOOM, subject.tilingZoomOf(CHEMINEE, null));
  }

  @Test
  void picks_the_mapping_of_the_requested_spatial_resolution_for_a_type_mapped_several_times() {
    var obstacles5cm =
        new DeployedModelCard("obstacles5", OBJECT_DETECTION, 1, false, null, null, null);
    var obstacles10cm =
        new DeployedModelCard("obstacles10", OBJECT_DETECTION, 1, false, null, null, null);
    var obstacles20cm =
        new DeployedModelCard("obstacles20", OBJECT_DETECTION, 1, false, null, null, null);
    when(conf.getMappings())
        .thenReturn(
            List.of(
                new ModelCardMapping(OBSTACLE, "obstacles5", 20, 5.0),
                new ModelCardMapping(OBSTACLE, "obstacles10", 19, 10.0),
                new ModelCardMapping(OBSTACLE, "obstacles20", 18, 20.0)));
    when(registry.findById("obstacles5")).thenReturn(Optional.of(obstacles5cm));
    when(registry.findById("obstacles10")).thenReturn(Optional.of(obstacles10cm));
    when(registry.findById("obstacles20")).thenReturn(Optional.of(obstacles20cm));
    var configs = configs(OBSTACLE);

    assertEquals(
        List.of(new ResolvedModelCard(obstacles5cm, 20, 5.0)),
        subject.resolve(configs, 5.0, 20, ANY).modelCards());
    assertEquals(
        List.of(new ResolvedModelCard(obstacles10cm, 19, 10.0)),
        subject.resolve(configs, 10.0, 19, ANY).modelCards());
    assertEquals(
        List.of(new ResolvedModelCard(obstacles20cm, 18, 20.0)),
        subject.resolve(configs, 20.0, 18, ANY).modelCards());
    assertTrue(subject.resolve(configs, 20.0, 20, ANY).modelCards().isEmpty());
    assertEquals(List.of(20), subject.tilingZoomLevels(configs, 5.0));
    assertEquals(List.of(19), subject.tilingZoomLevels(configs, 10.0));
    assertEquals(List.of(18), subject.tilingZoomLevels(configs, 20.0));
    assertEquals(18, subject.tilingZoomOf(OBSTACLE, 20.0));
    assertEquals(Set.of(5.0, 10.0, 20.0), subject.supportedSpatialResolutionsInCm(configs));
  }

  @Test
  void without_spatial_resolution_prefers_the_5_cm_mapping_then_the_first_one() {
    when(conf.getMappings())
        .thenReturn(
            List.of(
                new ModelCardMapping(OBSTACLE, "obstacles20", 19, 20.0),
                new ModelCardMapping(OBSTACLE, "obstacles5", 20, 5.0),
                new ModelCardMapping(TOMBE, "tombs20", 19, 20.0),
                new ModelCardMapping(TOMBE, "tombs10", 18, 10.0)));

    assertEquals(20, subject.tilingZoomOf(OBSTACLE, null));
    assertEquals(19, subject.tilingZoomOf(TOMBE, null));
  }

  @Test
  void never_falls_back_to_another_mapping_when_the_spatial_resolution_is_explicit() {
    when(conf.getMappings())
        .thenReturn(
            List.of(
                new ModelCardMapping(OBSTACLE, "obstacles5", 20, 5.0),
                new ModelCardMapping(OBSTACLE, "obstacles20", 19, 20.0)));
    var configs = configs(OBSTACLE);

    assertEquals(DEFAULT_ZOOM, subject.tilingZoomOf(OBSTACLE, 10.0));
    assertTrue(subject.resolve(configs, 10.0, 19, ANY).modelCards().isEmpty());
    assertTrue(subject.resolve(configs, 10.0, 20, ANY).modelCards().isEmpty());
  }

  @Test
  void at_5_cm_only_the_5_cm_model_card_is_called_and_at_20_cm_only_the_20_cm_one() {
    var obstacles5cm =
        new DeployedModelCard("obstacles5", OBJECT_DETECTION, 1, false, null, null, null);
    var obstacles20cm =
        new DeployedModelCard("obstacles20", OBJECT_DETECTION, 1, false, null, null, null);
    when(conf.getMappings())
        .thenReturn(
            List.of(
                new ModelCardMapping(OBSTACLE, "obstacles5", 21, 5.0),
                new ModelCardMapping(OBSTACLE, "obstacles20", 19, 20.0)));
    when(registry.findById("obstacles5")).thenReturn(Optional.of(obstacles5cm));
    when(registry.findById("obstacles20")).thenReturn(Optional.of(obstacles20cm));
    var configs = configs(OBSTACLE);

    assertEquals(List.of(21), subject.tilingZoomLevels(configs, 5.0));
    assertEquals(List.of(19), subject.tilingZoomLevels(configs, 20.0));
    assertEquals(
        List.of("obstacles5"),
        subject.resolve(configs, 5.0, 21, ANY).modelCards().stream()
            .map(ResolvedModelCard::modelCardId)
            .toList());
    assertTrue(subject.resolve(configs, 5.0, 19, ANY).modelCards().isEmpty());
    assertEquals(
        List.of("obstacles20"),
        subject.resolve(configs, 20.0, 19, ANY).modelCards().stream()
            .map(ResolvedModelCard::modelCardId)
            .toList());
    assertTrue(subject.resolve(configs, 20.0, 21, ANY).modelCards().isEmpty());
  }

  @Test
  void roof_equipment_types_tile_on_zoom_19_at_20_cm_and_on_their_5_cm_zoom_otherwise() {
    var equipment = List.of(OBSTACLE, CHEMINEE, VELUX, PANNEAU_PHOTOVOLTAIQUE);
    var mappings = new ArrayList<ModelCardMapping>();
    equipment.forEach(
        type -> {
          mappings.add(new ModelCardMapping(type, "roof-5cm", 21, 5.0));
          mappings.add(new ModelCardMapping(type, "roof-20cm", 19, 20.0));
        });
    when(conf.getMappings()).thenReturn(mappings);
    var configs = configs(equipment.toArray(DetectableType[]::new));

    assertEquals(List.of(19), subject.tilingZoomLevels(configs, 20.0));
    assertEquals(List.of(21), subject.tilingZoomLevels(configs, 5.0));
    assertEquals(Set.of(5.0, 20.0), subject.supportedSpatialResolutionsInCm(configs));
  }

  private static List<DetectableObjectConfiguration> configs(DetectableType... types) {
    return Arrays.stream(types)
        .<DetectableObjectConfiguration>map(
            type -> DetectableObjectConfiguration.builder().objectType(type).build())
        .toList();
  }
}
