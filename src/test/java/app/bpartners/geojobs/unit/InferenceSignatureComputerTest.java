package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PANNEAU_PHOTOVOLTAIQUE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.repository.ModelCardVersionRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.ModelCardVersion;
import app.bpartners.geojobs.service.detection.inference.InferenceModelCardConf;
import app.bpartners.geojobs.service.detection.inference.InferenceSignatureComputer;
import app.bpartners.geojobs.service.detection.inference.ModelCardMapping;
import app.bpartners.geojobs.service.detection.inference.ModelCardRegistry;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InferenceSignatureComputerTest {
  InferenceModelCardConf conf = mock();
  ModelCardRegistry registry = mock();
  ModelCardVersionRepository versionRepository = mock();
  InferenceSignatureComputer subject;

  @BeforeEach
  void setUp() {
    when(conf.getMappings())
        .thenReturn(
            List.of(
                new ModelCardMapping(TOITURE_REVETEMENT, "roof-card", 21, 5.0),
                new ModelCardMapping(ARBRE, "tree-card", 20, 5.0),
                new ModelCardMapping(PANNEAU_PHOTOVOLTAIQUE, "solar-card", 21, 5.0, 10.0)));
    subject =
        new InferenceSignatureComputer(new ModelCardResolver(conf, registry), versionRepository);
  }

  @Test
  void unversioned_model_cards_default_to_the_first_version() {
    when(versionRepository.findLatestByModelCardIdIn(any())).thenReturn(List.of());

    var actual = subject.compute(configurations(TOITURE_REVETEMENT), 5.0).orElseThrow();

    assertEquals("roof-card@5.0#v1", actual);
  }

  @Test
  void signature_uses_the_resolution_sent_to_the_inference_api() {
    when(versionRepository.findLatestByModelCardIdIn(any())).thenReturn(List.of());

    var actual = subject.compute(configurations(PANNEAU_PHOTOVOLTAIQUE), 5.0).orElseThrow();

    assertEquals("solar-card@10.0#v1", actual);
  }

  @Test
  void signature_lists_every_used_model_card_whatever_the_configuration_order() {
    when(versionRepository.findLatestByModelCardIdIn(any())).thenReturn(List.of());

    assertEquals(
        subject.compute(configurations(TOITURE_REVETEMENT, ARBRE), 5.0),
        subject.compute(configurations(ARBRE, TOITURE_REVETEMENT), 5.0));
    assertEquals(
        "roof-card@5.0#v1;tree-card@5.0#v1",
        subject.compute(configurations(ARBRE, TOITURE_REVETEMENT), 5.0).orElseThrow());
  }

  @Test
  void bumping_the_version_of_a_used_model_card_changes_the_signature() {
    when(versionRepository.findLatestByModelCardIdIn(any())).thenReturn(List.of());
    var before = subject.compute(configurations(TOITURE_REVETEMENT, ARBRE), 5.0);
    when(versionRepository.findLatestByModelCardIdIn(any()))
        .thenReturn(List.of(version("roof-card", 2)));

    var after = subject.compute(configurations(TOITURE_REVETEMENT, ARBRE), 5.0);

    assertNotEquals(before, after);
    assertEquals("roof-card@5.0#v2;tree-card@5.0#v1", after.orElseThrow());
  }

  @Test
  void the_latest_version_of_a_model_card_wins() {
    when(versionRepository.findLatestByModelCardIdIn(any()))
        .thenReturn(List.of(version("roof-card", 2), version("roof-card", 3)));

    assertEquals(
        "roof-card@5.0#v3", subject.compute(configurations(TOITURE_REVETEMENT), 5.0).orElseThrow());
  }

  @Test
  void bumping_the_version_of_an_unused_model_card_keeps_the_signature() {
    when(versionRepository.findLatestByModelCardIdIn(any())).thenReturn(List.of());
    var before = subject.compute(configurations(TOITURE_REVETEMENT), 5.0);
    when(versionRepository.findLatestByModelCardIdIn(any()))
        .thenReturn(List.of(version("tree-card", 7)));

    assertEquals(before, subject.compute(configurations(TOITURE_REVETEMENT), 5.0));
  }

  @Test
  void no_signature_without_mapped_object_types() {
    assertTrue(subject.compute(List.of(), 5.0).isEmpty());
    assertTrue(subject.compute(null, 5.0).isEmpty());
    assertTrue(subject.compute(configurations(DetectableType.PISCINE), 5.0).isEmpty());
  }

  private static ModelCardVersion version(String modelCardId, int version) {
    return ModelCardVersion.builder().modelCardId(modelCardId).version(version).build();
  }

  private static List<DetectableObjectConfiguration> configurations(DetectableType... types) {
    return Arrays.stream(types)
        .<DetectableObjectConfiguration>map(
            type -> DetectableObjectConfiguration.builder().objectType(type).build())
        .toList();
  }
}
