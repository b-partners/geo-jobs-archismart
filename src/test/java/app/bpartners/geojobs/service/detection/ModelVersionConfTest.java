package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.CLIMAT_RESILIENCE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.LOM;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.STATIONNEMENT;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.VEGETATION;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.ZAN;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PARKING;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PLACE_STANDARD;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.VELUX;
import static app.bpartners.geojobs.service.detection.DetectionApiVersion.V1;
import static app.bpartners.geojobs.service.detection.DetectionApiVersion.V2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.file.bucket.BucketConf;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.detection.inference.ModelCardMapping;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.s3.model.S3Object;

class ModelVersionConfTest {
  private static final String DUMMY_CONF =
      """
{
  "BP_TOITURE": [
    {"objectType": "TOITURE_REVETEMENT", "url": "https://dummy-toiture-v2", "version": "V2"},
    {"objectType": "VELUX", "url": "https://dummy-toiture-v2", "version": "V2"}
  ],
  "BP_VEGETATION": [],
  "BP_STATIONNEMENT": [
    {"objectType": "PLACE_STANDARD", "url": "https://dummy-stationnement-v1", "version": "V1"}
  ]
}""";

  private static final String BUCKET_NAME = "dummyBucket";
  private static final String CONF_PREFIX = "conf/null/model-versions/";
  private static final String MODEL_CONF_PREFIX = "conf/null/models/%s/";

  CustomBucketComponent bucketComponentMock = mock(CustomBucketComponent.class);
  BucketConf bucketConfMock = mock(BucketConf.class);
  ModelVersionConf subject = new ModelVersionConf(bucketComponentMock, new ObjectMapper());

  private static final String INFERENCE_CONF =
      """
{
  "BP_TOITURE": [
    {"objectType": "TOITURE_REVETEMENT", "modelCardId": "bp-toiture-revetements-v2.0",
     "zoomLevel": 21, "spatialResolution": 5},
    {"objectType": "TOITURE_REVETEMENT", "modelCardId": "revets-fp-v1",
     "zoomLevel": 19, "spatialResolution": 20},
    {"objectType": "VELUX", "modelCardId": "obstacles-only", "zoomLevel": 21,
     "spatialResolution": 20, "inferenceSpatialResolution": 5, "unknown": 1}
  ],
  "BP_STATIONNEMENT": [
    {"objectType": "PARKING", "url": "https://dummy-parking", "version": "V1"},
    {"objectType": "PLACE_STANDARD", "modelCardId": "stationnement-obb-2cls-v0",
     "zoomLevel": 20, "spatialResolution": 5}
  ]
}""";

  private final List<String> versions = new ArrayList<>();
  private final Map<String, List<String>> modelKeysByPrefix = new HashMap<>();

  @BeforeEach
  void setUp() {
    when(bucketComponentMock.getBucketConf()).thenReturn(bucketConfMock);
    when(bucketConfMock.getBucketName()).thenReturn(BUCKET_NAME);
    givenVersion("1.0.0", DUMMY_CONF);
  }

  @SneakyThrows
  private void givenVersion(String version, String content) {
    var confFile = Files.createTempFile("model-version", ".json");
    Files.writeString(confFile, content);
    versions.add(version);
    when(bucketComponentMock.listObjects(BUCKET_NAME, CONF_PREFIX))
        .thenReturn(
            versions.stream()
                .map(v -> S3Object.builder().key(CONF_PREFIX + v + ".json").build())
                .toList());
    when(bucketComponentMock.download(BUCKET_NAME, CONF_PREFIX + version + ".json"))
        .thenReturn(confFile.toFile());
  }

  @SneakyThrows
  private void givenModelVersion(ModelName modelName, String version, String content) {
    var confFile = Files.createTempFile("model-version", ".json");
    Files.writeString(confFile, content);
    var prefix = MODEL_CONF_PREFIX.formatted(modelName.getValue());
    var key = prefix + "versions/" + version + ".json";
    givenModelKey(prefix, key);
    when(bucketComponentMock.download(BUCKET_NAME, key)).thenReturn(confFile.toFile());
  }

  private void givenCurrent(ModelName modelName, String version) {
    givenCurrentPointer(modelName, "{\"version\": \"%s\"}".formatted(version));
  }

  @SneakyThrows
  private void givenCurrentPointer(ModelName modelName, String rawPointer) {
    var confFile = Files.createTempFile("model-current", ".json");
    Files.writeString(confFile, rawPointer);
    var prefix = MODEL_CONF_PREFIX.formatted(modelName.getValue());
    var key = prefix + "current.json";
    givenModelKey(prefix, key);
    when(bucketComponentMock.download(BUCKET_NAME, key)).thenReturn(confFile.toFile());
  }

  private void givenModelKey(String prefix, String key) {
    var keys = modelKeysByPrefix.computeIfAbsent(prefix, p -> new ArrayList<>());
    keys.add(key);
    when(bucketComponentMock.listObjects(BUCKET_NAME, prefix))
        .thenReturn(keys.stream().map(k -> S3Object.builder().key(k).build()).toList());
  }

  @Test
  void warm_up_loads_the_confs_so_that_resolving_afterwards_does_not_hit_the_bucket() {
    givenModelVersion(
        TOITURE,
        "2.0.0",
        "[{\"objectType\": \"VELUX\", \"url\": \"https://dummy\", \"version\": \"V2\"}]");
    givenCurrent(TOITURE, "2.0.0");

    subject.warmUp();
    clearInvocations(bucketComponentMock);

    assertEquals("2.0.0", subject.defaultVersion(TOITURE));
    subject.getInferenceMappings(List.of(model(TOITURE, null)));
    verify(bucketComponentMock, org.mockito.Mockito.never()).download(anyString(), anyString());
    verify(bucketComponentMock, org.mockito.Mockito.never()).listObjects(anyString(), anyString());
  }

  @Test
  void warm_up_does_not_throw_when_a_model_conf_is_broken() {
    givenModelVersion(TOITURE, "2.0.0", "not json");
    givenCurrent(TOITURE, "2.0.0");

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> subject.warmUp());
  }

  private DetectableObjectModel model(ModelName name, String version) {
    return new DetectableObjectModel().modelName(name).modelVersion(version);
  }

  @Test
  void splits_entries_between_inference_mappings_and_lambdas() {
    givenVersion("1.1.0", INFERENCE_CONF);

    assertEquals(
        List.of(
            new ModelCardMapping(TOITURE_REVETEMENT, "bp-toiture-revetements-v2.0", 21, 5.0),
            new ModelCardMapping(TOITURE_REVETEMENT, "revets-fp-v1", 19, 20.0),
            new ModelCardMapping(VELUX, "obstacles-only", 21, 20.0, 5.0)),
        subject.getInferenceMappings(List.of(model(TOITURE, "1.1.0"))));
    assertEquals(
        List.of(new ModelCardMapping(PLACE_STANDARD, "stationnement-obb-2cls-v0", 20, 5.0)),
        subject.getInferenceMappings(List.of(model(STATIONNEMENT, "1.1.0"))));
    var stationnementLambdas = subject.getTileDetectorUrls(STATIONNEMENT, "1.1.0");
    assertEquals(1, stationnementLambdas.size());
    assertEquals(PARKING, stationnementLambdas.getFirst().getObjectType());
  }

  @Test
  void has_no_inference_mapping_for_a_version_defining_only_lambdas() {
    assertTrue(subject.getInferenceMappings(List.of(model(TOITURE, "1.0.0"))).isEmpty());
  }

  @Test
  void uses_default_version_for_inference_mappings_when_no_model_provided() {
    givenVersion("1.1.0", INFERENCE_CONF);
    var toitureMappings =
        List.of(
            new ModelCardMapping(TOITURE_REVETEMENT, "bp-toiture-revetements-v2.0", 21, 5.0),
            new ModelCardMapping(TOITURE_REVETEMENT, "revets-fp-v1", 19, 20.0),
            new ModelCardMapping(VELUX, "obstacles-only", 21, 20.0, 5.0));

    assertEquals(4, subject.getInferenceMappings(List.of()).size());
    assertEquals(4, subject.getInferenceMappings(null).size());
    assertEquals(toitureMappings, subject.getInferenceMappings(List.of(model(TOITURE, null))));
  }

  @Test
  void uses_earliest_version_for_inference_mappings_of_a_legacy_model() {
    givenVersion("1.1.0", INFERENCE_CONF);

    assertTrue(subject.getInferenceMappings(List.of(model(TOITURE, "legacy"))).isEmpty());
  }

  @Test
  void selects_inference_mappings_of_each_model_with_its_own_version() {
    givenVersion("1.1.0", INFERENCE_CONF);

    var actual =
        subject.getInferenceMappings(
            List.of(model(TOITURE, "1.0.0"), model(STATIONNEMENT, "1.1.0")));

    assertEquals(
        List.of(new ModelCardMapping(PLACE_STANDARD, "stationnement-obb-2cls-v0", 20, 5.0)),
        actual);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        {"BP_TOITURE": [{"objectType": "VELUX", "modelCardId": "c", "zoomLevel": 21,
                         "spatialResolution": 5, "url": "https://u"}]}""",
        """
        {"BP_TOITURE": [{"objectType": "VELUX"}]}""",
        """
        {"BP_TOITURE": [{"objectType": "VELUX", "modelCardId": "c", "spatialResolution": 5}]}""",
        """
        {"BP_TOITURE": [
          {"objectType": "VELUX", "modelCardId": "a", "zoomLevel": 21, "spatialResolution": 5},
          {"objectType": "VELUX", "modelCardId": "b", "zoomLevel": 21, "spatialResolution": 5}
        ]}"""
      })
  void rejects_invalid_model_version_conf(String invalidConf) {
    givenVersion("1.1.0", invalidConf);
    var models = List.of(model(TOITURE, "1.1.0"));

    assertThrows(IllegalStateException.class, () -> subject.getInferenceMappings(models));
  }

  @Test
  void resolves_default_version_when_none_provided() {
    assertEquals("1.1.0", subject.resolveVersion(TOITURE, null));
    assertEquals("1.1.0", subject.resolveVersion(TOITURE, ""));
    assertEquals("1.0.0", subject.resolveVersion(TOITURE, "1.0.0"));
  }

  @Test
  void resolves_legacy_version_as_the_earliest_version() {
    givenVersion("1.1.0", INFERENCE_CONF);

    assertEquals("1.0.0", subject.resolveVersion(TOITURE, "legacy"));
  }

  @Test
  void does_not_accept_legacy_as_a_requested_version() {
    givenVersion("1.1.0", INFERENCE_CONF);

    assertFalse(subject.hasConfiguration(TOITURE, "legacy"));
    assertFalse(subject.hasConfiguration(TOITURE, "not-a-version"));
  }

  @Test
  void throws_when_unsupported_version_resolved() {
    var actual =
        assertThrows(BadRequestException.class, () -> subject.resolveVersion(TOITURE, "2.0.0"));
    assertEquals("Unsupported model version 2.0.0", actual.getMessage());
  }

  @Test
  void loads_toiture_urls_from_s3_conf() {
    var urls = subject.getTileDetectorUrls(TOITURE, "1.0.0");

    assertEquals(2, urls.size());
    var toitureRevetement =
        urls.stream()
            .filter(u -> u.getObjectType() == TOITURE_REVETEMENT)
            .findFirst()
            .orElseThrow();
    assertEquals("https://dummy-toiture-v2", toitureRevetement.getUrl());
    assertEquals(V2, toitureRevetement.getVersion());
  }

  @Test
  void loads_stationnement_urls_from_s3_conf() {
    var urls = subject.getTileDetectorUrls(STATIONNEMENT, "1.0.0");

    assertEquals(1, urls.size());
    var placeStandard =
        urls.stream().filter(u -> u.getObjectType() == PLACE_STANDARD).findFirst().orElseThrow();
    assertEquals("https://dummy-stationnement-v1", placeStandard.getUrl());
    assertEquals(V1, placeStandard.getVersion());
  }

  @Test
  void vegetation_is_defined_and_relies_on_default_endpoint() {
    assertTrue(subject.hasConfiguration(VEGETATION, "1.0.0"));
    assertTrue(subject.getTileDetectorUrls(VEGETATION, "1.0.0").isEmpty());
  }

  @Test
  void has_no_configuration_for_undefined_model() {
    givenVersion("1.1.0", INFERENCE_CONF);

    assertFalse(subject.hasConfiguration(LOM, null));
    assertFalse(subject.hasConfiguration(LOM, "1.0.0"));
    assertTrue(subject.hasConfiguration(TOITURE, null));
  }

  @Test
  void exposes_available_versions_per_model() {
    assertEquals(Set.of("1.0.0"), subject.getAvailableVersions(TOITURE));
    assertEquals(Set.of("1.0.0"), subject.getAvailableVersions(VEGETATION));
    assertEquals(Set.of(), subject.getAvailableVersions(LOM));
  }

  private static final String TOITURE_MODEL_CONF =
      """
[
  {"objectType": "TOITURE_REVETEMENT", "modelCardId": "bp-toiture-revetements-v3.0",
   "zoomLevel": 21, "spatialResolution": 5}
]""";

  @Test
  void reads_a_model_from_its_own_versioned_directory() {
    givenModelVersion(TOITURE, "2.0.0", TOITURE_MODEL_CONF);

    assertEquals(
        List.of(new ModelCardMapping(TOITURE_REVETEMENT, "bp-toiture-revetements-v3.0", 21, 5.0)),
        subject.getInferenceMappings(List.of(model(TOITURE, "2.0.0"))));
    assertEquals(Set.of("1.0.0", "2.0.0"), subject.getAvailableVersions(TOITURE));
  }

  @Test
  void versions_models_independently_of_each_other() {
    givenVersion("1.1.0", INFERENCE_CONF);
    givenModelVersion(TOITURE, "2.0.0", TOITURE_MODEL_CONF);

    assertEquals(Set.of("1.0.0", "1.1.0", "2.0.0"), subject.getAvailableVersions(TOITURE));
    assertEquals(Set.of("1.0.0", "1.1.0"), subject.getAvailableVersions(STATIONNEMENT));
    assertEquals(
        List.of(new ModelCardMapping(PLACE_STANDARD, "stationnement-obb-2cls-v0", 20, 5.0)),
        subject.getInferenceMappings(List.of(model(STATIONNEMENT, "1.1.0"))));
  }

  @Test
  void prefers_the_model_directory_over_the_legacy_file_for_the_same_version() {
    givenVersion("1.1.0", INFERENCE_CONF);
    givenModelVersion(TOITURE, "1.1.0", TOITURE_MODEL_CONF);

    assertEquals(
        List.of(new ModelCardMapping(TOITURE_REVETEMENT, "bp-toiture-revetements-v3.0", 21, 5.0)),
        subject.getInferenceMappings(List.of(model(TOITURE, "1.1.0"))));
  }

  @Test
  void uses_current_pointer_as_default_version_of_a_model() {
    givenVersion("1.1.0", INFERENCE_CONF);
    givenModelVersion(TOITURE, "2.0.0", TOITURE_MODEL_CONF);
    givenCurrent(TOITURE, "2.0.0");

    assertEquals("2.0.0", subject.defaultVersion(TOITURE));
    assertEquals("2.0.0", subject.resolveVersion(TOITURE, null));
    assertEquals("1.1.0", subject.defaultVersion(STATIONNEMENT));
    assertEquals(
        List.of(new ModelCardMapping(TOITURE_REVETEMENT, "bp-toiture-revetements-v3.0", 21, 5.0)),
        subject.getInferenceMappings(List.of(model(TOITURE, null))));
    assertEquals(
        List.of(
            new ModelCardMapping(TOITURE_REVETEMENT, "bp-toiture-revetements-v3.0", 21, 5.0),
            new ModelCardMapping(PLACE_STANDARD, "stationnement-obb-2cls-v0", 20, 5.0)),
        subject.getInferenceMappings(List.of()));
  }

  @Test
  void rejects_a_current_pointer_to_an_unpublished_version() {
    givenModelVersion(TOITURE, "2.0.0", TOITURE_MODEL_CONF);
    givenCurrent(TOITURE, "3.0.0");

    assertThrows(IllegalStateException.class, () -> subject.defaultVersion(TOITURE));
  }

  @Test
  void resolves_legacy_as_the_earliest_version_of_the_model() {
    givenModelVersion(LOM, "0.5.0", "[]");

    assertEquals("0.5.0", subject.resolveVersion(LOM, "legacy"));
    assertEquals("1.0.0", subject.resolveVersion(TOITURE, "legacy"));
  }

  @Test
  void does_not_resolve_a_version_published_for_another_model() {
    givenModelVersion(TOITURE, "2.0.0", TOITURE_MODEL_CONF);

    assertFalse(subject.hasConfiguration(STATIONNEMENT, "2.0.0"));
    assertThrows(
        BadRequestException.class, () -> subject.getTileDetectorUrls(STATIONNEMENT, "2.0.0"));
  }

  @Test
  void falls_back_to_the_default_version_for_an_unnamed_model() {
    assertEquals("1.1.0", subject.defaultVersion(null));
    assertEquals(Set.of(), subject.getAvailableVersions(null));
    assertFalse(subject.hasConfiguration(null, null));
  }

  @Test
  void treats_a_blank_requested_version_as_the_default_version() {
    givenVersion("1.1.0", INFERENCE_CONF);

    assertTrue(subject.hasConfiguration(TOITURE, ""));
    assertTrue(subject.hasConfiguration(TOITURE, " "));
  }

  @Test
  void throws_when_the_default_version_has_no_configuration_for_the_model() {
    var actual =
        assertThrows(BadRequestException.class, () -> subject.getTileDetectorUrls(LOM, null));

    assertEquals("Model BP_LOM has no configuration for version 1.1.0", actual.getMessage());
  }

  @Test
  void ignores_selected_models_without_a_name_when_collecting_inference_mappings() {
    givenVersion("1.1.0", INFERENCE_CONF);

    var actual =
        subject.getInferenceMappings(List.of(model(null, "1.0.0"), model(STATIONNEMENT, "1.1.0")));

    assertEquals(
        List.of(new ModelCardMapping(PLACE_STANDARD, "stationnement-obb-2cls-v0", 20, 5.0)),
        actual);
  }

  @Test
  void throws_when_legacy_is_requested_for_a_model_without_any_version() {
    var actual =
        assertThrows(BadRequestException.class, () -> subject.resolveVersion(LOM, "legacy"));

    assertTrue(
        actual.getMessage().startsWith("No model version configuration found for model BP_LOM"));
  }

  @Test
  void ignores_unexpected_keys_in_the_model_directory() {
    var prefix = MODEL_CONF_PREFIX.formatted(LOM.getValue());
    givenModelKey(prefix, prefix + "readme.md");
    givenModelKey(prefix, prefix + "versions/notes.txt");
    givenModelKey(prefix, prefix + "versions/nested/1.0.0.json");
    givenModelKey(prefix, prefix + "versions/.json");
    givenModelKey(prefix, prefix + "versions/1.2.0.json");

    assertEquals(Set.of("1.2.0"), subject.getAvailableVersions(LOM));
  }

  @Test
  void ignores_unexpected_keys_in_the_legacy_directory() {
    when(bucketComponentMock.listObjects(BUCKET_NAME, CONF_PREFIX))
        .thenReturn(
            List.of(
                    CONF_PREFIX + "1.0.0.json",
                    CONF_PREFIX + "notes.txt",
                    CONF_PREFIX + "archive/0.9.0.json",
                    CONF_PREFIX + ".json")
                .stream()
                .map(key -> S3Object.builder().key(key).build())
                .toList());

    assertEquals(Set.of("1.0.0"), subject.getAvailableVersions(TOITURE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"version\": \" \"}"})
  void rejects_a_current_pointer_without_a_version(String rawPointer) {
    givenCurrentPointer(TOITURE, rawPointer);

    var actual = assertThrows(IllegalStateException.class, () -> subject.defaultVersion(TOITURE));

    assertTrue(actual.getMessage().endsWith("must define a version"));
  }

  @Test
  void rejects_a_malformed_current_pointer() {
    givenCurrentPointer(TOITURE, "not json");

    assertThrows(JsonProcessingException.class, () -> subject.defaultVersion(TOITURE));
  }

  @Test
  void rejects_an_invalid_conf_in_the_model_directory() {
    givenModelVersion(TOITURE, "2.0.0", "[{\"objectType\": \"VELUX\"}]");
    var models = List.of(model(TOITURE, "2.0.0"));

    assertThrows(IllegalStateException.class, () -> subject.getInferenceMappings(models));
  }

  @Test
  void propagates_a_failure_to_download_a_model_conf() {
    var prefix = MODEL_CONF_PREFIX.formatted(TOITURE.getValue());
    var key = prefix + "versions/2.0.0.json";
    givenModelKey(prefix, key);
    when(bucketComponentMock.download(BUCKET_NAME, key))
        .thenThrow(new IllegalStateException("boom"));
    var models = List.of(model(TOITURE, "2.0.0"));

    var actual =
        assertThrows(IllegalStateException.class, () -> subject.getInferenceMappings(models));

    assertEquals("boom", actual.getMessage());
  }

  @Test
  void orders_versions_with_a_different_number_of_parts() {
    var lomPrefix = MODEL_CONF_PREFIX.formatted(LOM.getValue());
    givenModelKey(lomPrefix, lomPrefix + "versions/1.0.1.json");
    givenModelKey(lomPrefix, lomPrefix + "versions/1.0.json");
    var zanPrefix = MODEL_CONF_PREFIX.formatted(ZAN.getValue());
    givenModelKey(zanPrefix, zanPrefix + "versions/1.0.json");
    givenModelKey(zanPrefix, zanPrefix + "versions/1.0.1.json");
    var climatPrefix = MODEL_CONF_PREFIX.formatted(CLIMAT_RESILIENCE.getValue());
    givenModelKey(climatPrefix, climatPrefix + "versions/2.json");
    givenModelKey(climatPrefix, climatPrefix + "versions/1.0.1.json");

    assertEquals(List.of("1.0", "1.0.1"), List.copyOf(subject.getAvailableVersions(LOM)));
    assertEquals(List.of("1.0", "1.0.1"), List.copyOf(subject.getAvailableVersions(ZAN)));
    assertEquals(
        List.of("1.0.1", "2"), List.copyOf(subject.getAvailableVersions(CLIMAT_RESILIENCE)));
  }
}
