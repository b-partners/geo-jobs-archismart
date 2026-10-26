package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE_NOIRCIE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.VELUX;
import static app.bpartners.geojobs.service.detection.inference.DeployedModelCard.ProblemType.OBJECT_DETECTION;
import static app.bpartners.geojobs.service.detection.inference.DeployedModelCard.ProblemType.SEMANTIC_SEGMENTATION;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.bucket.BucketConf;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.repository.DetectionFileObjectRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.inference.DeployedModelCard;
import app.bpartners.geojobs.service.detection.inference.InferenceApiClient;
import app.bpartners.geojobs.service.detection.inference.InferenceRequest;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver.Resolution;
import app.bpartners.geojobs.service.detection.inference.ResolvedModelCard;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class HttpApiTileObjectDetectorTest {
  private static final String BUCKET_NAME = "dummyBucketName";
  private static final String TILE_BUCKET_PATH = "dummyTileBucketPath";
  private static final String V1_API_URL = "https://v1.detection.test";
  private static final String FIRST_V2_API_URL = "https://first-v2.detection.test";
  private static final String SECOND_V2_API_URL = "https://second-v2.detection.test";
  private static final DeployedModelCard DAMAGES_CARD =
      new DeployedModelCard("damages", SEMANTIC_SEGMENTATION, 1, true, 1024, 1024, true);
  private static final DeployedModelCard TOMBS_CARD_WITHOUT_SIZE =
      new DeployedModelCard("tombs", OBJECT_DETECTION, 1, false, null, null, true);
  private static final DeployedModelCard TOMBS_CARD =
      new DeployedModelCard("tombs", OBJECT_DETECTION, 1, false, null, null, true);
  private static final ResolvedModelCard RESOLVED_DAMAGES =
      new ResolvedModelCard(DAMAGES_CARD, 21, 5.0);
  private static final ResolvedModelCard RESOLVED_TOMBS =
      new ResolvedModelCard(TOMBS_CARD, 20, 10.0);

  private final CustomBucketComponent customBucketComponentMock = mock();
  private final TileObjectDetectorConf tileObjectDetectorConfMock = mock();
  private final DetectionFileObjectRepository detectionFileObjectRepositoryMock = mock();
  private final BucketComponent bucketComponentMock = mock();
  private final FileWriter fileWriterMock = mock();
  private final RestTemplate restTemplateMock = mock();
  private final InferenceApiClient inferenceApiClientMock = mock();
  private final ModelCardResolver modelCardResolverMock = mock();
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private byte[] tileBytes;

  private final HttpApiTileObjectDetector subject =
      new HttpApiTileObjectDetector(
          objectMapper,
          customBucketComponentMock,
          "https://default.detection.test",
          tileObjectDetectorConfMock,
          new DetectionResponseAggregator(),
          new DetectionResponseAggregatorV1(),
          new DetectionResponseV1ToV2Mapper(),
          detectionFileObjectRepositoryMock,
          bucketComponentMock,
          fileWriterMock,
          objectMapper,
          restTemplateMock,
          inferenceApiClientMock,
          modelCardResolverMock);

  @SneakyThrows
  @Test
  void calls_all_detection_apis_concurrently() {
    setUpTile();
    resolveNothing();
    var concurrentCallsLatch = new CountDownLatch(3);
    var calledUrls = ConcurrentHashMap.<String>newKeySet();
    answerOnceEveryApiIsCalled(concurrentCallsLatch, calledUrls);

    assertDoesNotThrow(() -> subject.apply(tileDetectionTask(), null, detectableObjectConfs()));

    assertEquals(Set.of(V1_API_URL, FIRST_V2_API_URL, SECOND_V2_API_URL), calledUrls);
  }

  @SneakyThrows
  @Test
  void aggregates_every_api_response_whatever_their_completion_order() {
    setUpTile();
    resolveNothing();
    var concurrentCallsLatch = new CountDownLatch(3);
    answerOnceEveryApiIsCalled(concurrentCallsLatch, ConcurrentHashMap.newKeySet());

    var actual = subject.apply(tileDetectionTask(), null, detectableObjectConfs());

    assertEquals(3, actual.getImages().size());
    assertTrue(
        actual.getImages().values().stream()
            .allMatch(imageData -> imageData.getRegions().size() == 1));
    verify(restTemplateMock, times(1))
        .postForEntity(eq(V1_API_URL), any(), eq(DetectionResponse.class));
    verify(restTemplateMock, times(2))
        .postForEntity(anyString(), any(), eq(DetectionResponseV2.class));
  }

  @Test
  void calls_inference_api_only_for_mapped_types_and_never_the_lambdas() {
    setUpTile();
    when(modelCardResolverMock.resolve(any(), any(), anyInt(), any()))
        .thenReturn(new Resolution(List.of(RESOLVED_DAMAGES, RESOLVED_TOMBS), Set.of(), Set.of()));
    when(inferenceApiClientMock.infer(any()))
        .thenAnswer(
            invocation -> v2Response(((InferenceRequest) invocation.getArgument(0)).modelCardId()));
    var mask = tempFile(new byte[] {4, 5, 6});

    var actual = subject.apply(tileDetectionTask(), mask, detectableObjectConfs());

    var requestCaptor = ArgumentCaptor.forClass(InferenceRequest.class);
    verify(inferenceApiClientMock, times(2)).infer(requestCaptor.capture());
    var requestsByModelCard =
        requestCaptor.getAllValues().stream()
            .collect(Collectors.toMap(InferenceRequest::modelCardId, request -> request));
    var damagesRequest = requestsByModelCard.get("damages");
    var tombsRequest = requestsByModelCard.get("tombs");
    assertEquals(Base64.getEncoder().encodeToString(tileBytes), damagesRequest.image1());
    assertEquals(Base64.getEncoder().encodeToString(new byte[] {4, 5, 6}), damagesRequest.mask1());
    assertEquals(21, damagesRequest.zoomLevel());
    assertEquals(5.0, damagesRequest.spatialResolution());
    assertEquals(20, tombsRequest.zoomLevel());
    assertEquals(10.0, tombsRequest.spatialResolution());
    assertNull(tombsRequest.mask1());
    assertNull(tombsRequest.image2());
    verify(restTemplateMock, never()).postForEntity(anyString(), any(), any());
    assertEquals(2, actual.getImages().size());
    var sources =
        actual.getImages().values().stream()
            .flatMap(image -> image.getRegions().values().stream())
            .map(region -> region.getRegionAttributes().get("source_url"))
            .collect(Collectors.toSet());
    assertEquals(Set.of("inference:damages", "inference:tombs"), sources);
  }

  @Test
  void falls_back_to_lambdas_only_for_unmapped_types() {
    setUpTile();
    when(modelCardResolverMock.resolve(any(), any(), anyInt(), any()))
        .thenReturn(new Resolution(List.of(RESOLVED_DAMAGES), Set.of(PISCINE), Set.of()));
    when(inferenceApiClientMock.infer(any())).thenReturn(v2Response("damages"));
    when(restTemplateMock.postForEntity(anyString(), any(), eq(DetectionResponse.class)))
        .thenReturn(new ResponseEntity<>(v1Response(V1_API_URL), HttpStatus.OK));

    var actual =
        subject.apply(tileDetectionTask(), tempFile(new byte[] {4}), detectableObjectConfs());

    verify(inferenceApiClientMock, times(1)).infer(any());
    verify(restTemplateMock, times(1))
        .postForEntity(eq(V1_API_URL), any(), eq(DetectionResponse.class));
    verify(restTemplateMock, never())
        .postForEntity(eq(FIRST_V2_API_URL), any(), eq(DetectionResponseV2.class));
    verify(restTemplateMock, never())
        .postForEntity(eq(SECOND_V2_API_URL), any(), eq(DetectionResponseV2.class));
    assertEquals(2, actual.getImages().size());
  }

  @Test
  void keeps_other_results_when_one_inference_call_fails() {
    setUpTile();
    when(modelCardResolverMock.resolve(any(), any(), anyInt(), any()))
        .thenReturn(new Resolution(List.of(RESOLVED_DAMAGES, RESOLVED_TOMBS), Set.of(), Set.of()));
    when(inferenceApiClientMock.infer(any()))
        .thenAnswer(
            invocation -> {
              var request = (InferenceRequest) invocation.getArgument(0);
              if (request.modelCardId().equals("damages")) {
                throw new IllegalStateException("inference api down");
              }
              return v2Response(request.modelCardId());
            });

    var actual =
        subject.apply(tileDetectionTask(), tempFile(new byte[] {4}), detectableObjectConfs());

    assertEquals(1, actual.getImages().size());
  }

  @Test
  void fails_when_mask_required_but_missing() {
    setUpTile();
    when(modelCardResolverMock.resolve(any(), any(), anyInt(), any()))
        .thenReturn(new Resolution(List.of(RESOLVED_DAMAGES), Set.of(), Set.of()));

    assertThrows(
        IllegalStateException.class,
        () -> subject.apply(tileDetectionTask(), null, detectableObjectConfs()));
    verify(inferenceApiClientMock, never()).infer(any());
  }

  @Test
  void accepts_model_cards_matching_the_tile_size_or_the_default_512() {
    var supports = capturedCompatibilityPredicate(png(512, 512));

    assertTrue(supports.test(TOMBS_CARD_WITHOUT_SIZE));
    assertTrue(supports.test(card("small", 512, 512)));
    assertFalse(supports.test(card("large", 1024, 1024)));
    assertFalse(supports.test(card("tall", 512, 1024)));
  }

  @Test
  void accepts_model_cards_declaring_the_tile_size() {
    var supports = capturedCompatibilityPredicate(png(1024, 1024));

    assertTrue(supports.test(card("large", 1024, 1024)));
    assertFalse(supports.test(card("small", 512, 512)));
    assertFalse(supports.test(TOMBS_CARD_WITHOUT_SIZE));
  }

  @Test
  void rejects_every_model_card_when_tile_size_is_unreadable() {
    var supports = capturedCompatibilityPredicate(new byte[] {1, 2, 3});

    assertFalse(supports.test(TOMBS_CARD_WITHOUT_SIZE));
    assertFalse(supports.test(card("small", 512, 512)));
  }

  @Test
  void falls_back_to_lambdas_for_types_whose_model_card_does_not_support_the_tile_size() {
    setUpTile(png(1024, 1024));
    when(modelCardResolverMock.resolve(any(), any(), anyInt(), any()))
        .thenReturn(
            new Resolution(List.of(), Set.of(), Set.of(PISCINE, VELUX, MOISISSURE_NOIRCIE)));
    answerOnceEveryApiIsCalled(new CountDownLatch(3), ConcurrentHashMap.newKeySet());

    var actual = subject.apply(tileDetectionTask(), null, detectableObjectConfs());

    verify(inferenceApiClientMock, never()).infer(any());
    assertEquals(3, actual.getImages().size());
  }

  @SuppressWarnings("unchecked")
  private Predicate<DeployedModelCard> capturedCompatibilityPredicate(byte[] tile) {
    setUpTile(tile);
    when(modelCardResolverMock.resolve(any(), any(), anyInt(), any()))
        .thenReturn(new Resolution(List.of(), Set.of(), Set.of()));
    subject.apply(tileDetectionTask(), null, detectableObjectConfs());
    var captor = ArgumentCaptor.forClass(Predicate.class);
    verify(modelCardResolverMock).resolve(any(), any(), anyInt(), captor.capture());
    return captor.getValue();
  }

  private static DeployedModelCard card(String id, int width, int height) {
    return new DeployedModelCard(id, SEMANTIC_SEGMENTATION, 1, false, width, height, true);
  }

  @Test
  void resolves_model_cards_for_the_zoom_of_the_tile() {
    setUpTile();
    when(modelCardResolverMock.resolve(any(), any(), anyInt(), any()))
        .thenReturn(new Resolution(List.of(), Set.of(), Set.of()));
    var task = tileDetectionTask();
    task.getTile().setCoordinates(new TileCoordinates().x(1).y(1).z(21));

    subject.apply(task, null, detectableObjectConfs());

    verify(modelCardResolverMock).resolve(any(), any(), eq(21), any());
  }

  @Test
  void never_falls_back_to_detection_lambdas_when_the_spatial_resolution_is_not_5_cm() {
    setUpTile();
    resolveNothing();
    var task = tileDetectionTask();
    task.setSpatialResolutionInCm(20.0);

    assertThrows(
        IllegalStateException.class, () -> subject.apply(task, null, detectableObjectConfs()));
  }

  @Test
  void resolves_model_cards_for_the_spatial_resolution_of_the_detection() {
    setUpTile();
    when(modelCardResolverMock.resolve(any(), any(), anyInt(), any()))
        .thenReturn(new Resolution(List.of(), Set.of(), Set.of()));
    var task = tileDetectionTask();
    task.setSpatialResolutionInCm(20.0);

    subject.apply(task, null, detectableObjectConfs());

    verify(modelCardResolverMock).resolve(any(), eq(20.0), anyInt(), any());
  }

  private void resolveNothing() {
    when(modelCardResolverMock.resolve(any(), any(), anyInt(), any()))
        .thenReturn(
            new Resolution(List.of(), Set.of(PISCINE, VELUX, MOISISSURE_NOIRCIE), Set.of()));
  }

  @SneakyThrows
  private File tempFile(byte[] content) {
    var file = File.createTempFile("mask", ".png");
    Files.write(file.toPath(), content);
    file.deleteOnExit();
    return file;
  }

  private void answerOnceEveryApiIsCalled(CountDownLatch latch, Set<String> calledUrls) {
    when(restTemplateMock.postForEntity(anyString(), any(), eq(DetectionResponseV2.class)))
        .thenAnswer(
            invocation -> {
              String apiUrl = invocation.getArgument(0);
              calledUrls.add(apiUrl);
              awaitEveryOtherCall(latch);
              return new ResponseEntity<>(v2Response(apiUrl), HttpStatus.OK);
            });
    when(restTemplateMock.postForEntity(anyString(), any(), eq(DetectionResponse.class)))
        .thenAnswer(
            invocation -> {
              String apiUrl = invocation.getArgument(0);
              calledUrls.add(apiUrl);
              awaitEveryOtherCall(latch);
              return new ResponseEntity<>(v1Response(apiUrl), HttpStatus.OK);
            });
  }

  @SneakyThrows
  private static void awaitEveryOtherCall(CountDownLatch latch) {
    latch.countDown();
    if (!latch.await(10, SECONDS)) {
      throw new IllegalStateException("detection apis were called sequentially");
    }
  }

  private DetectionResponseV2 v2Response(String apiUrl) {
    return DetectionResponseV2.builder()
        .images(
            new java.util.HashMap<>(
                java.util.Map.of(
                    apiUrl,
                    DetectionResponseV2.ImageData.builder()
                        .filename(apiUrl)
                        .regions(new java.util.HashMap<>(java.util.Map.of("region_1", region())))
                        .build())))
        .build();
  }

  private DetectionResponseV2.ImageData.Region region() {
    return DetectionResponseV2.ImageData.Region.builder()
        .regionAttributes(new java.util.HashMap<>())
        .shapeAttributes(
            DetectionResponseV2.ImageData.ShapeAttributes.builder()
                .name("polygon")
                .allPointsX(List.of(java.math.BigDecimal.ZERO))
                .allPointsY(List.of(java.math.BigDecimal.ZERO))
                .build())
        .build();
  }

  private DetectionResponse v1Response(String apiUrl) {
    return DetectionResponse.builder()
        .rstRaw(
            new java.util.HashMap<>(
                java.util.Map.of(
                    apiUrl,
                    DetectionResponse.ImageData.builder()
                        .filename(apiUrl)
                        .regions(new java.util.HashMap<>(java.util.Map.of("region_1", v1Region())))
                        .build())))
        .build();
  }

  private DetectionResponse.ImageData.Region v1Region() {
    return DetectionResponse.ImageData.Region.builder()
        .regionAttributes(new java.util.HashMap<>())
        .shapeAttributes(
            DetectionResponse.ImageData.ShapeAttributes.builder()
                .name("polygon")
                .allPointsX(List.of(java.math.BigDecimal.ZERO))
                .allPointsY(List.of(java.math.BigDecimal.ZERO))
                .build())
        .build();
  }

  private void setUpTile() {
    setUpTile(png(512, 512));
  }

  @SneakyThrows
  private static byte[] png(int width, int height) {
    var output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(width, height, TYPE_INT_RGB), "png", output);
    return output.toByteArray();
  }

  @SneakyThrows
  private void setUpTile(byte[] content) {
    tileBytes = content;
    var bucketConfMock = mock(BucketConf.class);
    var tileImageFile = File.createTempFile("tile", ".jpg");
    Files.write(tileImageFile.toPath(), content);
    tileImageFile.deleteOnExit();

    when(bucketConfMock.getBucketName()).thenReturn(BUCKET_NAME);
    when(customBucketComponentMock.getBucketConf()).thenReturn(bucketConfMock);
    when(customBucketComponentMock.download(BUCKET_NAME, TILE_BUCKET_PATH))
        .thenReturn(tileImageFile);
    when(tileObjectDetectorConfMock.getTileDetectionApiUrls()).thenReturn(tileDetectionApiUrls());
  }

  private TileDetectionTask tileDetectionTask() {
    return TileDetectionTask.builder()
        .id(randomUUID().toString())
        .jobId(randomUUID().toString())
        .detectionIdentifier(randomUUID().toString())
        .tile(
            Tile.builder()
                .id(randomUUID().toString())
                .bucketPath(TILE_BUCKET_PATH)
                .coordinates(new TileCoordinates().x(1).y(1).z(20))
                .build())
        .build();
  }

  private List<DetectableObjectConfiguration> detectableObjectConfs() {
    return List.of(
        detectableObjectConf(PISCINE),
        detectableObjectConf(VELUX),
        detectableObjectConf(MOISISSURE_NOIRCIE));
  }

  private DetectableObjectConfiguration detectableObjectConf(DetectableType objectType) {
    return DetectableObjectConfiguration.builder()
        .id(randomUUID().toString())
        .objectType(objectType)
        .build();
  }

  private String tileDetectionApiUrls() {
    return """
           [
             {"objectType": "PISCINE", "url": "%s", "version": "V1"},
             {"objectType": "VELUX", "url": "%s", "version": "V2"},
             {"objectType": "MOISISSURE_NOIRCIE", "url": "%s", "version": "V2"}
           ]"""
        .formatted(V1_API_URL, FIRST_V2_API_URL, SECOND_V2_API_URL);
  }
}
