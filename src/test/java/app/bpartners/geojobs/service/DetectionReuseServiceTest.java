package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.ASSEMBLE_VGG;
import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.TILE_MASK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.concurrency.Workers;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionTrackingRegistrationRequested;
import app.bpartners.geojobs.endpoint.rest.model.DelimitationType;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerProperties;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.bucket.BucketObjectCopier;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.DetectionFileObject;
import app.bpartners.geojobs.repository.model.detection.DetectionFileType;
import app.bpartners.geojobs.service.detection.inference.InferenceSignatureComputer;
import app.bpartners.geojobs.service.event.DetectionSucceededService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import software.amazon.awssdk.services.s3.model.S3Object;

class DetectionReuseServiceTest {
  static final String SQUARE = "[[[2.0,48.0],[2.001,48.0],[2.001,48.001],[2.0,48.001],[2.0,48.0]]]";
  static final String SHIFTED_40_CM =
      "[[[2.000004,48.000004],[2.001004,48.000004],[2.001004,48.001004],[2.000004,48.001004],"
          + "[2.000004,48.000004]]]";
  static final String SHIFTED_80_CM =
      "[[[2.000008,48.000008],[2.001008,48.000008],[2.001008,48.001008],[2.000008,48.001008],"
          + "[2.000008,48.000008]]]";
  static final String SHIFTED_3_M =
      "[[[2.00003,48.00003],[2.00103,48.00003],[2.00103,48.00103],[2.00003,48.00103],"
          + "[2.00003,48.00003]]]";
  static final String OTHER =
      "[[[2.01,48.0],[2.011,48.0],[2.011,48.001],[2.01,48.001],[2.01,48.0]]]";

  DetectionRepository detectionRepository = mock();
  InferenceSignatureComputer signatureComputer = mock();
  BucketObjectCopier bucketObjectCopier = mock();
  BucketComponent bucketComponent = mock();
  CustomBucketComponent customBucketComponent = mock();
  EventProducer eventProducer = mock();
  DetectionSucceededService detectionSucceededService = mock();
  DetectionReuseService subject =
      new DetectionReuseService(
          detectionRepository,
          signatureComputer,
          bucketObjectCopier,
          bucketComponent,
          customBucketComponent,
          new Workers(),
          eventProducer,
          detectionSucceededService,
          ZoneSimilarityConf.defaults());

  @BeforeEach
  void setUp() {
    when(detectionRepository.save(any(Detection.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void fingerprint_of_an_individual_zone_holds_the_zone_hash_and_the_inference_signature() {
    when(signatureComputer.compute(any(), any(), any())).thenReturn(Optional.of("card@5.0#v1"));

    var actual = subject.fingerprintOf(target(SQUARE));

    assertTrue(actual.zoneHash().startsWith("zone-v1:"));
    assertEquals("card@5.0#v1", actual.inferenceSignature());
  }

  @Test
  void fingerprint_of_a_zone_shifted_by_a_few_decimeters_is_the_same() {
    when(signatureComputer.compute(any(), any(), any())).thenReturn(Optional.of("card@5.0#v1"));
    var shifted =
        "[[[2.000004,48.000004],[2.001004,48.000004],[2.001004,48.001004],[2.000004,48.001004],"
            + "[2.000004,48.000004]]]";

    assertEquals(
        subject.fingerprintOf(target(SQUARE)).zoneHash(),
        subject.fingerprintOf(target(shifted)).zoneHash());
    assertNotEquals(
        subject.fingerprintOf(target(SQUARE)).zoneHash(),
        subject.fingerprintOf(target(OTHER)).zoneHash());
  }

  @Test
  void fingerprint_has_no_hash_for_several_geometries() {
    var detection =
        target(SQUARE).toBuilder()
            .providedGeoJsonZone(List.of(polygon(SQUARE), polygon(OTHER)))
            .build();

    var actual = subject.fingerprintOf(detection);

    assertNull(actual.zoneHash());
    assertNull(actual.inferenceSignature());
    verifyNoInteractions(signatureComputer);
  }

  @Test
  void fingerprint_keeps_the_hash_when_the_signature_cannot_be_computed() {
    when(signatureComputer.compute(any(), any(), any())).thenThrow(new IllegalStateException("s3"));

    var actual = subject.fingerprintOf(target(SQUARE));

    assertTrue(actual.zoneHash().startsWith("zone-v1:"));
    assertNull(actual.inferenceSignature());
  }

  @Test
  void nothing_is_reused_without_a_fingerprint_or_for_an_already_processed_detection() {
    var withoutHash = target(SQUARE).toBuilder().zoneHash(null).build();
    var withoutSignature = target(SQUARE).toBuilder().inferenceSignature(null).build();
    var withoutCommunity = target(SQUARE).toBuilder().communityOwnerId(null).build();
    var withResults = target(SQUARE).toBuilder().geojsonS3FileKey("geoJson/t/z.zip").build();
    var withTiling = target(SQUARE).toBuilder().ztjId("ztj").build();

    for (var detection :
        List.of(withoutHash, withoutSignature, withoutCommunity, withResults, withTiling)) {
      assertTrue(subject.reuseResultsOf(detection).isEmpty());
    }

    verifyNoInteractions(detectionRepository, bucketObjectCopier);
  }

  @Test
  void nothing_is_reused_when_no_succeeded_detection_matches() {
    whenCandidates(List.of());

    assertTrue(subject.reuseResultsOf(target(SQUARE)).isEmpty());

    verifyNoInteractions(bucketObjectCopier, eventProducer);
    verify(detectionRepository, never()).save(any());
  }

  @Test
  void results_of_the_latest_compatible_detection_are_copied_into_the_target() {
    var latest = source("latest", "L1");
    var older = source("older", "L1");
    whenCandidates(List.of(latest, older));

    var actual = subject.reuseResultsOf(target(SQUARE)).orElseThrow();

    assertEquals("latest", actual.getSourceDetectionId());
    assertEquals("geoJson/target/zone.zip", actual.getGeojsonS3FileKey());
    assertEquals("vgg/target/zone.json", actual.getVggFileKey());
    assertEquals(200, actual.getImageWidth());
    assertEquals(100, actual.getImageHeight());
    verify(bucketObjectCopier).copy("geoJson/latest/zone.zip", "geoJson/target/zone.zip");
    verify(bucketObjectCopier).copy("vgg/latest/zone.json", "vgg/target/zone.json");
    verify(bucketObjectCopier, never()).copy(eq("geoJson/older/zone.zip"), anyString());
  }

  @Test
  void only_the_result_file_objects_are_copied_under_new_identifiers() {
    var source = source("latest", "L1");
    whenCandidates(List.of(source));

    var actual = subject.reuseResultsOf(target(SQUARE)).orElseThrow();

    assertEquals(1, actual.getFileObjects().size());
    var copiedFileObject = actual.getFileObjects().getFirst();
    assertEquals("target", copiedFileObject.getDetectionIdentifier());
    assertEquals("vgg/target/zone.json", copiedFileObject.getBucketKey());
    assertEquals(ASSEMBLE_VGG, copiedFileObject.getFileType());
    assertNotEquals("file-object", copiedFileObject.getId());
    verify(bucketObjectCopier, never()).copy(eq("tiles/latest/tile.jpg"), anyString());
  }

  @Test
  void a_source_shared_by_several_fields_is_copied_once() {
    var source = source("latest", "L1");
    whenCandidates(List.of(source));

    subject.reuseResultsOf(target(SQUARE));

    verify(bucketObjectCopier).copy("vgg/latest/zone.json", "vgg/target/zone.json");
    verify(bucketObjectCopier, times(2)).copy(anyString(), anyString());
  }

  @Test
  void candidates_on_another_layer_are_skipped() {
    whenCandidates(List.of(source("other-layer", "L2"), source("same-layer", "L1")));

    var actual = subject.reuseResultsOf(target(SQUARE)).orElseThrow();

    assertEquals("same-layer", actual.getSourceDetectionId());
  }

  @Test
  void candidates_with_other_objects_or_confidence_are_skipped() {
    var otherConfidence =
        source("other-confidence", "L1").toBuilder()
            .detectableObjectConfigurations(List.of(configuration(0.9)))
            .build();
    var otherObjects =
        source("other-objects", "L1").toBuilder()
            .detectableObjectConfigurations(
                List.of(
                    configuration(0.5),
                    DetectableObjectConfiguration.builder()
                        .objectType(
                            app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE)
                        .build()))
            .build();
    whenCandidates(List.of(otherConfidence, otherObjects));

    assertTrue(subject.reuseResultsOf(target(SQUARE)).isEmpty());
  }

  @Test
  void candidates_with_another_delimitation_or_output_format_are_skipped() {
    var otherDelimitation =
        source("delimitation", "L1").toBuilder()
            .geoJsonDelimitationType(DelimitationType.PARCEL)
            .build();
    var zippedSource = source("zipped", "L1").toBuilder().isOutputZipped(true).build();
    whenCandidates(List.of(otherDelimitation, zippedSource));

    assertTrue(subject.reuseResultsOf(target(SQUARE)).isEmpty());
  }

  @Test
  void a_source_without_image_cannot_serve_a_detection_needing_one() {
    whenCandidates(List.of(sourceWithFeature("no-image")));
    whenZoneImages("no-image");

    assertTrue(subject.reuseResultsOf(targetNeedingImage()).isEmpty());
    verify(bucketObjectCopier, never()).copy(anyString(), anyString());
  }

  @Test
  void zone_images_of_the_source_are_copied_under_the_target_identifiers() {
    whenCandidates(List.of(sourceWithFeature("latest")));
    whenZoneImages(
        "latest", "zone_images/latest.jpg", "zone_images/latest/feature-source/zone-source.jpg");

    var actual = subject.reuseResultsOf(targetNeedingImage());

    assertTrue(actual.isPresent());
    verify(bucketObjectCopier).copy("zone_images/latest.jpg", "zone_images/target.jpg");
    verify(bucketObjectCopier)
        .copy(
            "zone_images/latest/feature-source/zone-source.jpg",
            "zone_images/target/feature-target/zone-target.jpg");
  }

  @Test
  void a_source_with_only_the_single_zone_image_serves_a_detection_needing_one() {
    whenCandidates(List.of(sourceWithFeature("latest")));
    whenZoneImages("latest", "zone_images/latest.jpg");

    assertTrue(subject.reuseResultsOf(targetNeedingImage()).isPresent());
    verify(bucketObjectCopier).copy("zone_images/latest.jpg", "zone_images/target.jpg");
  }

  @Test
  void zone_images_are_not_copied_when_the_target_does_not_need_an_image() {
    whenCandidates(List.of(sourceWithFeature("latest")));
    whenZoneImages("latest", "zone_images/latest.jpg");

    assertTrue(subject.reuseResultsOf(target(SQUARE)).isPresent());
    verify(bucketObjectCopier, never()).copy(eq("zone_images/latest.jpg"), anyString());
  }

  @Test
  void a_copy_failure_removes_the_copied_files_and_falls_back_to_processing() {
    whenCandidates(List.of(source("latest", "L1")));
    doThrow(new IllegalStateException("s3 down"))
        .when(bucketObjectCopier)
        .copy(eq("vgg/latest/zone.json"), anyString());

    assertTrue(subject.reuseResultsOf(target(SQUARE)).isEmpty());

    verify(detectionRepository, never()).save(any());
    verify(bucketObjectCopier).deleteQuietly("geoJson/target/zone.zip");
    verify(bucketObjectCopier, never()).deleteQuietly("vgg/target/zone.json");
    verifyNoInteractions(eventProducer);
  }

  @Test
  void a_save_failure_removes_the_copied_files_and_falls_back_to_processing() {
    whenCandidates(List.of(source("latest", "L1")));
    when(detectionRepository.save(any(Detection.class))).thenThrow(new IllegalStateException("db"));

    assertTrue(subject.reuseResultsOf(target(SQUARE)).isEmpty());

    verify(bucketObjectCopier).deleteQuietly("geoJson/target/zone.zip");
    verify(bucketObjectCopier).deleteQuietly("vgg/target/zone.json");
  }

  @Test
  void an_asynchronous_copy_notifies_the_client_and_registers_the_analysis() {
    whenCandidates(List.of(source("latest", "L1")));

    var actual = subject.reuseResultsOf(target(SQUARE)).orElseThrow();

    verify(detectionSucceededService).acceptReused(actual);
    var events = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(events.capture());
    assertTrue(
        events.getValue().getFirst() instanceof DetectionTrackingRegistrationRequested event
            && "target".equals(event.getDetectionIdentifier()));
  }

  @Test
  void a_synchronous_copy_only_registers_the_analysis() {
    whenCandidates(List.of(source("latest", "L1")));

    subject.reuseResultsOf(target(SQUARE).toBuilder().isSynchronous(true).build());

    verifyNoInteractions(detectionSucceededService);
    verify(eventProducer).accept(any());
  }

  @Test
  void a_notification_failure_does_not_discard_the_copy() {
    whenCandidates(List.of(source("latest", "L1")));
    doThrow(new IllegalStateException("mail")).when(detectionSucceededService).acceptReused(any());

    var actual = subject.reuseResultsOf(target(SQUARE));

    assertTrue(actual.isPresent());
    verify(bucketObjectCopier, never()).deleteQuietly(anyString());
  }

  @Test
  void the_lookup_is_scoped_to_the_community_the_fingerprint_and_the_test_flag() {
    whenCandidates(List.of());

    subject.reuseResultsOf(target(SQUARE));

    verify(detectionRepository)
        .findReusableSourceIds(
            eq("community"),
            eq("hash"),
            eq("signature"),
            eq(false),
            eq("target"),
            any(Pageable.class));
  }

  @Test
  void destination_key_replaces_the_source_identifier_or_prefixes_the_target_one() {
    var source = Detection.builder().id("source").build();
    var target = Detection.builder().id("target").build();

    assertEquals(
        "geoJson/target/zone.zip",
        DetectionReuseService.destinationKey(source, target, "geoJson/source/zone.zip"));
    assertEquals(
        "zip/target/combined.zip",
        DetectionReuseService.destinationKey(source, target, "zip/combined.zip"));
    assertEquals(
        "reused/target/file.json",
        DetectionReuseService.destinationKey(source, target, "file.json"));
  }

  @Test
  void a_target_with_the_same_layer_but_no_layer_at_all_is_never_compatible() {
    var withoutLayer = target(SQUARE).toBuilder().geoServerProperties(null).build();

    assertFalse(DetectionReuseService.isCompatible(source("s", null), withoutLayer));
  }

  private void whenZoneImages(String sourceId, String... keys) {
    when(bucketComponent.getBucketName()).thenReturn("bucket");
    when(customBucketComponent.listObjects("bucket", "zone_images/" + sourceId))
        .thenReturn(
            java.util.Arrays.stream(keys).map(key -> S3Object.builder().key(key).build()).toList());
  }

  private static Detection targetNeedingImage() {
    return target(SQUARE).toBuilder()
        .needsImageOutput(true)
        .zoneName("zone-target")
        .providedGeoJsonZone(List.of(polygonWithId(SQUARE, "feature-target")))
        .build();
  }

  private static Detection sourceWithFeature(String id) {
    return source(id, "L1").toBuilder()
        .zoneName("zone-source")
        .providedGeoJsonZone(List.of(polygonWithId(SQUARE, "feature-source")))
        .build();
  }

  private static Feature polygonWithId(String coordinates, String id) {
    var feature = polygon(coordinates);
    feature.setProperties(java.util.Map.of("id", id));
    return feature;
  }

  @Test
  void fingerprint_holds_the_bounds_of_the_zone() {
    when(signatureComputer.compute(any(), any(), any())).thenReturn(Optional.of("card@5.0#v1"));

    var bounds = subject.fingerprintOf(target(SQUARE)).zoneBounds();

    assertEquals(2.0, bounds.minLon());
    assertEquals(48.0, bounds.minLat());
    assertEquals(2.001, bounds.maxLon());
    assertEquals(48.001, bounds.maxLat());
  }

  @Test
  void a_zone_shifted_by_less_than_the_tolerance_reuses_the_nearby_detection() {
    whenNearby(List.of(nearbySource("near", SHIFTED_40_CM)));

    var actual = subject.reuseResultsOf(target(SQUARE)).orElseThrow();

    assertEquals("near", actual.getSourceDetectionId());
    verify(bucketObjectCopier).copy("geoJson/near/zone.zip", "geoJson/target/zone.zip");
  }

  @Test
  void any_of_the_hand_drawn_zones_of_the_same_building_reuses_the_results_of_another() {
    var zones = ZoneGeometryRealZonesTest.ZONES;
    for (int sourceIndex = 0; sourceIndex < zones.size(); sourceIndex++) {
      for (int targetIndex = 0; targetIndex < zones.size(); targetIndex++) {
        if (sourceIndex == targetIndex) {
          continue;
        }
        whenNearby(List.of(nearbySource("source-" + sourceIndex, zones.get(sourceIndex))));

        var actual = subject.reuseResultsOf(target(zones.get(targetIndex)));

        assertEquals(
            "source-" + sourceIndex,
            actual
                .orElseThrow(
                    () ->
                        new AssertionError(
                            "a zone of the same building was processed instead of reused"))
                .getSourceDetectionId());
      }
    }
  }

  @Test
  void a_zone_shifted_by_more_than_the_tolerance_is_processed() {
    whenNearby(List.of(nearbySource("far", SHIFTED_3_M)));

    assertTrue(subject.reuseResultsOf(target(SQUARE)).isEmpty());
    verifyNoInteractions(bucketObjectCopier);
  }

  @Test
  void the_nearby_detection_with_the_best_overlap_is_reused() {
    whenNearby(
        List.of(
            nearbySource("less-aligned", SHIFTED_80_CM), nearbySource("aligned", SHIFTED_40_CM)));

    var actual = subject.reuseResultsOf(target(SQUARE)).orElseThrow();

    assertEquals("aligned", actual.getSourceDetectionId());
  }

  @Test
  void nearby_detections_on_another_layer_are_skipped() {
    var otherLayer =
        nearbySource("other-layer", SHIFTED_40_CM).toBuilder()
            .geoServerProperties(properties("L2"))
            .build();
    whenNearby(List.of(otherLayer));

    assertTrue(subject.reuseResultsOf(target(SQUARE)).isEmpty());
  }

  @Test
  void nearby_detections_are_not_searched_when_approximate_reuse_is_disabled() {
    var disabled =
        new DetectionReuseService(
            detectionRepository,
            signatureComputer,
            bucketObjectCopier,
            bucketComponent,
            customBucketComponent,
            new Workers(),
            eventProducer,
            detectionSucceededService,
            new ZoneSimilarityConf(false, 0.95, 1.0));
    whenNearby(List.of(nearbySource("near", SHIFTED_40_CM)));

    assertTrue(disabled.reuseResultsOf(target(SQUARE)).isEmpty());
    verify(detectionRepository, never())
        .findReusableSourceIdsNear(
            anyString(),
            anyString(),
            anyBoolean(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            any(Pageable.class));
  }

  private void whenNearby(List<Detection> candidates) {
    when(detectionRepository.findReusableSourceIdsNear(
            anyString(),
            anyString(),
            anyBoolean(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            any(Pageable.class)))
        .thenReturn(candidates.stream().map(Detection::getId).toList());
    candidates.forEach(
        candidate ->
            when(detectionRepository.findById(candidate.getId()))
                .thenReturn(Optional.of(candidate)));
  }

  private static Detection nearbySource(String id, String coordinates) {
    return source(id, "L1").toBuilder().providedGeoJsonZone(List.of(polygon(coordinates))).build();
  }

  private void whenCandidates(List<Detection> candidates) {
    when(detectionRepository.findReusableSourceIds(
            anyString(), anyString(), anyString(), anyBoolean(), anyString(), any(Pageable.class)))
        .thenReturn(candidates.stream().map(Detection::getId).toList());
    candidates.forEach(
        candidate ->
            when(detectionRepository.findById(candidate.getId()))
                .thenReturn(Optional.of(candidate)));
  }

  private static Detection target(String coordinates) {
    return Detection.builder()
        .id("target")
        .endToEndId("target-e2e")
        .communityOwnerId("community")
        .zoneHash("hash")
        .inferenceSignature("signature")
        .providedGeoJsonZone(List.of(polygon(coordinates)))
        .multiPolygonGeoJsonZone(List.of(polygon(coordinates)))
        .geoServerProperties(properties("L1"))
        .detectableObjectConfigurations(List.of(configuration(0.5)))
        .geoJsonDelimitationType(DelimitationType.PARCEL_FREE_DELIMITATION)
        .build();
  }

  private static Detection source(String id, String layer) {
    return Detection.builder()
        .id(id)
        .communityOwnerId("community")
        .zoneHash("hash")
        .inferenceSignature("signature")
        .geoServerProperties(properties(layer))
        .detectableObjectConfigurations(List.of(configuration(0.5)))
        .geoJsonDelimitationType(DelimitationType.PARCEL_FREE_DELIMITATION)
        .geojsonS3FileKey("geoJson/" + id + "/zone.zip")
        .vggFileKey("vgg/" + id + "/zone.json")
        .imageWidth(200)
        .imageHeight(100)
        .fileObjects(
            List.of(
                fileObject(ASSEMBLE_VGG, "vgg/" + id + "/zone.json"),
                fileObject(TILE_MASK, "tiles/" + id + "/tile.jpg")))
        .build();
  }

  private static DetectionFileObject fileObject(DetectionFileType type, String key) {
    return DetectionFileObject.builder()
        .id("file-object")
        .detectionIdentifier("source")
        .bucketKey(key)
        .fileName("zone")
        .fileType(type)
        .build();
  }

  private static DetectableObjectConfiguration configuration(double confidence) {
    return DetectableObjectConfiguration.builder()
        .objectType(TOITURE_REVETEMENT)
        .minConfidenceForDetection(confidence)
        .build();
  }

  private static GeoServerProperties properties(String layer) {
    return new GeoServerProperties()
        .geoServerParameter(new GeoServerParameter().layers(layer).precisionLevelInCm(5));
  }

  private static Feature polygon(String coordinates) {
    return Feature.builder()
        .geometry(
            Feature.FeatureGeometry.builder()
                .actualInstanceStringValue(
                    "{\"type\":\"Polygon\",\"coordinates\":" + coordinates + "}")
                .build())
        .build();
  }
}
