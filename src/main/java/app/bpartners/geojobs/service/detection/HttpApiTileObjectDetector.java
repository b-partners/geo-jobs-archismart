package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectableObjectTypeMapper.detectableObjectTypeForVegetationModel;
import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.TILE_DETECTION_RESULT_V1;
import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.TILE_DETECTION_RESULT_V2;
import static app.bpartners.geojobs.service.detection.DetectionApiVersion.V2;
import static java.lang.System.currentTimeMillis;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.repository.DetectionFileObjectRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectionFileObject;
import app.bpartners.geojobs.repository.model.detection.DetectionFileType;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.inference.DeployedModelCard;
import app.bpartners.geojobs.service.detection.inference.InferenceApiClient;
import app.bpartners.geojobs.service.detection.inference.InferenceRequest;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import app.bpartners.geojobs.service.detection.inference.ResolvedModelCard;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(value = "objects.detector.mock.activated", havingValue = "false")
@Slf4j
public class HttpApiTileObjectDetector implements TileObjectDetector {
  private static final String INFERENCE_SOURCE_PREFIX = "inference:";
  private static final int DEFAULT_SUPPORTED_TILE_SIZE = 512;
  private final ObjectMapper om;
  private final CustomBucketComponent customBucketComponent;
  private final String defaultDetectionApiUrl;
  private final TileObjectDetectorConf tileObjectDetectorConf;
  private final DetectionResponseAggregator detectionResponseAggregator;
  private final DetectionResponseAggregatorV1 detectionResponseAggregatorV1;
  private final DetectionResponseV1ToV2Mapper detectionResponseV1ToV2Mapper;
  private final DetectionFileObjectRepository detectionFileObjectRepository;
  private final FileWriter fileWriter;
  private final ObjectMapper objectMapper;
  private final BucketComponent bucketComponent;
  private final RestTemplate restTemplate;
  private final InferenceApiClient inferenceApiClient;
  private final ModelCardResolver modelCardResolver;

  @SneakyThrows
  public HttpApiTileObjectDetector(
      ObjectMapper om,
      CustomBucketComponent customBucketComponent,
      @Value("${tile.detection.api.url}") String defaultApiUrl,
      TileObjectDetectorConf tileObjectDetectorConf,
      DetectionResponseAggregator detectionResponseAggregator,
      DetectionResponseAggregatorV1 detectionResponseAggregatorV1,
      DetectionResponseV1ToV2Mapper detectionResponseV1ToV2Mapper,
      DetectionFileObjectRepository detectionFileObjectRepository,
      BucketComponent bucketComponent,
      FileWriter fileWriter,
      ObjectMapper objectMapper,
      RestTemplate restTemplate,
      InferenceApiClient inferenceApiClient,
      ModelCardResolver modelCardResolver) {
    this.om = om;
    this.customBucketComponent = customBucketComponent;
    this.defaultDetectionApiUrl = defaultApiUrl;
    this.tileObjectDetectorConf = tileObjectDetectorConf;
    this.detectionResponseAggregator = detectionResponseAggregator;
    this.detectionResponseAggregatorV1 = detectionResponseAggregatorV1;
    this.detectionResponseV1ToV2Mapper = detectionResponseV1ToV2Mapper;
    this.detectionFileObjectRepository = detectionFileObjectRepository;
    this.bucketComponent = bucketComponent;
    this.fileWriter = fileWriter;
    this.objectMapper = objectMapper;
    this.restTemplate = restTemplate;
    this.inferenceApiClient = inferenceApiClient;
    this.modelCardResolver = modelCardResolver;
  }

  @SneakyThrows
  @Override
  public DetectionResponseV2 apply(
      TileDetectionTask tileDetectionTask,
      File mask,
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    Tile tile = tileDetectionTask.getTile();
    var isDebugMode = tileDetectionTask.isDebugMode();
    if (tile == null) {
      return null;
    }
    var tileImageBucketPath = tile.getBucketPath();
    var tileImageFile =
        customBucketComponent.download(
            customBucketComponent.getBucketConf().getBucketName(), tileImageBucketPath);
    String base64ImgData = Base64.getEncoder().encodeToString(readFileToByteArray(tileImageFile));
    String base64MaskData =
        mask == null ? null : Base64.getEncoder().encodeToString(readFileToByteArray(mask));

    var tileSize = readImageSize(tileImageFile);
    var resolution =
        modelCardResolver.resolve(
            detectableObjectConfigurations,
            tileDetectionTask.getSpatialResolutionInCm(),
            tileZoom(tile),
            modelCard -> supportsTileSize(modelCard, tileSize));
    var v2Responses = new ArrayList<DetectionResponseAggregator.DetectionResponseUrl>();
    var v1Responses = new ArrayList<DetectionResponseAggregatorV1.DetectionResponseUrl>();

    for (var inferenceCall :
        callInferenceApi(
            resolution.modelCards(), tileImageFile.getName(), base64ImgData, base64MaskData)) {
      v2Responses.add(inferenceCall);
      if (isDebugMode) {
        persistDetectionResponse(
            tileDetectionTask,
            tileImageBucketPath,
            inferenceCall.detectionResponse(),
            "_response_v2_",
            TILE_DETECTION_RESULT_V2);
      }
    }

    var lambdaTypes = new HashSet<>(resolution.unmappedTypes());
    lambdaTypes.addAll(resolution.unsupportedTypes());
    var lambdaConfigurations = lambdaConfigurations(detectableObjectConfigurations, lambdaTypes);
    if (!lambdaConfigurations.isEmpty()) {
      var spatialResolutionInCm = tileDetectionTask.getSpatialResolutionInCm();
      if (spatialResolutionInCm != null
          && spatialResolutionInCm != ModelCardResolver.DEFAULT_SPATIAL_RESOLUTION_IN_CM) {
        throw new IllegalStateException(
            "Detection lambdas only support a spatial resolution of "
                + ModelCardResolver.DEFAULT_SPATIAL_RESOLUTION_IN_CM
                + " cm, but "
                + lambdaTypes
                + " cannot be detected by inference model cards at "
                + spatialResolutionInCm
                + " cm (tile size "
                + tileSize
                + ")");
      }
      if (modelCardResolver.isInferenceApiEnabled()) {
        if (!resolution.unmappedTypes().isEmpty()) {
          log.warn(
              "No inference model card mapped for {}, falling back to detection lambdas",
              resolution.unmappedTypes());
        }
        if (!resolution.unsupportedTypes().isEmpty()) {
          log.warn(
              "Tile size {} not supported by the inference model cards of {}, falling back to"
                  + " detection lambdas",
              tileSize,
              resolution.unsupportedTypes());
        }
      }
      callLambdaApis(
          tileDetectionTask,
          tileImageFile.getName(),
          tileImageBucketPath,
          base64ImgData,
          base64MaskData,
          lambdaConfigurations,
          v1Responses,
          v2Responses);
    }

    return mergeToV2(
        detectionResponseAggregatorV1.apply(v1Responses),
        detectionResponseAggregator.apply(v2Responses));
  }

  static int tileZoom(Tile tile) {
    var coordinates = tile.getCoordinates();
    return coordinates == null || coordinates.getZ() == null
        ? ModelCardResolver.DEFAULT_TILING_ZOOM
        : coordinates.getZ();
  }

  static boolean supportsTileSize(DeployedModelCard modelCard, Dimension tileSize) {
    if (tileSize == null) {
      return false;
    }
    var expectedWidth =
        modelCard.imageWidth() == null ? DEFAULT_SUPPORTED_TILE_SIZE : modelCard.imageWidth();
    var expectedHeight =
        modelCard.imageHeight() == null ? DEFAULT_SUPPORTED_TILE_SIZE : modelCard.imageHeight();
    return tileSize.width == expectedWidth && tileSize.height == expectedHeight;
  }

  static Dimension readImageSize(File imageFile) {
    try (var input = ImageIO.createImageInputStream(imageFile)) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        log.warn("Unable to read the size of tile image {}", imageFile.getName());
        return null;
      }
      var reader = readers.next();
      try {
        reader.setInput(input);
        return new Dimension(reader.getWidth(0), reader.getHeight(0));
      } finally {
        reader.dispose();
      }
    } catch (IOException e) {
      log.warn("Unable to read the size of tile image {}: {}", imageFile.getName(), e.getMessage());
      return null;
    }
  }

  private List<DetectableObjectConfiguration> lambdaConfigurations(
      List<DetectableObjectConfiguration> objectConfigurations, Set<DetectableType> unmappedTypes) {
    return objectConfigurations.stream()
        .filter(configuration -> unmappedTypes.contains(configuration.getObjectType()))
        .toList();
  }

  @SneakyThrows
  private void callLambdaApis(
      TileDetectionTask tileDetectionTask,
      String tileImageName,
      String tileImageBucketPath,
      String base64ImgData,
      String base64MaskData,
      List<DetectableObjectConfiguration> lambdaConfigurations,
      List<DetectionResponseAggregatorV1.DetectionResponseUrl> v1Responses,
      List<DetectionResponseAggregator.DetectionResponseUrl> v2Responses) {
    var isDebugMode = tileDetectionTask.isDebugMode();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);
    boolean vegetation = hasVegetationModel(lambdaConfigurations);
    var requestV2 =
        new HttpEntity<>(
            om.writeValueAsString(
                buildPayloadV2(tileImageName, base64ImgData, base64MaskData, vegetation)),
            headers);
    var requestV1 =
        new HttpEntity<>(
            om.writeValueAsString(
                buildPayloadV1(
                    tileDetectionTask.getJobId(),
                    tileImageName,
                    base64ImgData,
                    base64MaskData,
                    vegetation)),
            headers);

    var detectionApiUrls = getApiUrls(lambdaConfigurations);
    var detectionApiCalls = callApisInParallel(detectionApiUrls, requestV1, requestV2);
    for (var detectionApiCall : detectionApiCalls) {
      var apiUrl = detectionApiCall.apiUrl().getUrl();
      if (detectionApiCall.v1Response() != null) {
        v1Responses.add(
            new DetectionResponseAggregatorV1.DetectionResponseUrl(
                detectionApiCall.v1Response(), apiUrl));
        if (isDebugMode) {
          persistDetectionResponse(
              tileDetectionTask,
              tileImageBucketPath,
              detectionApiCall.v1Response(),
              "_response_v1_",
              TILE_DETECTION_RESULT_V1);
        }
      } else if (detectionApiCall.v2Response() != null) {
        v2Responses.add(
            new DetectionResponseAggregator.DetectionResponseUrl(
                detectionApiCall.v2Response(), apiUrl));
        if (isDebugMode) {
          persistDetectionResponse(
              tileDetectionTask,
              tileImageBucketPath,
              detectionApiCall.v2Response(),
              "_response_v2_",
              TILE_DETECTION_RESULT_V2);
        }
      }
    }
  }

  private List<DetectionResponseAggregator.DetectionResponseUrl> callInferenceApi(
      List<ResolvedModelCard> modelCards,
      String fileName,
      String base64ImgData,
      String base64MaskData) {
    try (var executor = newVirtualThreadPerTaskExecutor()) {
      var futures =
          modelCards.stream()
              .map(
                  modelCard ->
                      executor.submit(
                          () ->
                              callInferenceApi(modelCard, fileName, base64ImgData, base64MaskData)))
              .toList();
      return futures.stream()
          .map(HttpApiTileObjectDetector::awaitFuture)
          .filter(Objects::nonNull)
          .toList();
    }
  }

  private DetectionResponseAggregator.DetectionResponseUrl callInferenceApi(
      ResolvedModelCard resolvedModelCard,
      String fileName,
      String base64ImgData,
      String base64MaskData) {
    var modelCard = resolvedModelCard.modelCard();
    if (modelCard.imagesRequired() != 1) {
      throw new IllegalStateException(
          "Model card "
              + modelCard.modelCardId()
              + " requires several images, which is unsupported");
    }
    if (modelCard.maskRequired() && base64MaskData == null) {
      throw new IllegalStateException(
          "Model card " + modelCard.modelCardId() + " requires a mask but none was provided");
    }
    var request =
        InferenceRequest.builder()
            .modelCardId(modelCard.modelCardId())
            .image1(base64ImgData)
            .mask1(modelCard.maskRequired() ? base64MaskData : null)
            .filename(fileName)
            .zoomLevel(resolvedModelCard.zoomLevel())
            .spatialResolution(resolvedModelCard.spatialResolution())
            .build();
    log.info("Attempting to call inference API with model card {}", modelCard.modelCardId());
    try {
      var response = inferenceApiClient.infer(request);
      return response == null
          ? null
          : new DetectionResponseAggregator.DetectionResponseUrl(
              response, INFERENCE_SOURCE_PREFIX + modelCard.modelCardId());
    } catch (IllegalStateException e) {
      log.error(
          "Error while calling inference API with model card {}: {}",
          modelCard.modelCardId(),
          e.getMessage());
      return null;
    }
  }

  private List<DetectionApiCall> callApisInParallel(
      List<TileDetectorUrl> detectionApiUrls,
      HttpEntity<String> requestV1,
      HttpEntity<String> requestV2) {
    try (var executor = newVirtualThreadPerTaskExecutor()) {
      var futures =
          detectionApiUrls.stream()
              .map(apiUrl -> executor.submit(() -> callApi(apiUrl, requestV1, requestV2)))
              .toList();
      return futures.stream().map(HttpApiTileObjectDetector::awaitFuture).toList();
    }
  }

  private DetectionApiCall callApi(
      TileDetectorUrl apiUrl, HttpEntity<String> requestV1, HttpEntity<String> requestV2) {
    if (apiUrl.getVersion() == DetectionApiVersion.V1) {
      return new DetectionApiCall(
          apiUrl, callApi(requestV1, apiUrl.getUrl(), DetectionResponse.class), null);
    }
    return new DetectionApiCall(
        apiUrl, null, callApi(requestV2, apiUrl.getUrl(), DetectionResponseV2.class));
  }

  @SneakyThrows
  private static <T> T awaitFuture(Future<T> future) {
    try {
      return future.get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @SneakyThrows
  private void persistDetectionResponse(
      TileDetectionTask tileDetectionTask,
      String tileImageBucketPath,
      Object detectionResponse,
      String suffixPrefix,
      DetectionFileType fileType) {
    var detectionResponseBytes = objectMapper.writeValueAsBytes(detectionResponse);
    var suffix = suffixPrefix + currentTimeMillis();
    var tileDetectionResultBucketKey = insertSuffix(tileImageBucketPath, suffix, ".json");
    var fileName = replaceSeparator(tileDetectionResultBucketKey);

    bucketComponent.upload(
        fileWriter.write(detectionResponseBytes, createTempDirectory(), fileName),
        tileDetectionResultBucketKey);

    detectionFileObjectRepository.save(
        DetectionFileObject.builder()
            .id(randomUUID().toString())
            .fileName(fileName)
            .bucketKey(tileDetectionResultBucketKey)
            .detectionIdentifier(tileDetectionTask.getDetectionIdentifier())
            .fileType(fileType)
            .creationDatetime(now())
            .build());
  }

  private <T> T callApi(HttpEntity<String> request, String apiUrl, Class<T> responseType) {
    UriComponentsBuilder uriBuilder;
    try {
      uriBuilder = UriComponentsBuilder.fromUri(new URI(apiUrl));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    log.info("Attempting to call API for detection {} ", apiUrl);
    ResponseEntity<T> responseEntity;
    try {
      responseEntity = restTemplate.postForEntity(uriBuilder.toUriString(), request, responseType);
    } catch (HttpStatusCodeException e) {
      log.error(
          "Error while calling API for detection {} with exception {}", apiUrl, e.getMessage());
      return null;
    }
    if (responseEntity.getStatusCode().value() == 200) {
      return responseEntity.getBody();
    }
    log.error("Error while calling API for detection {} ", apiUrl);
    return null;
  }

  /**
   * Merges the (optional) aggregated V1 response - converted to V2 - with the aggregated V2
   * response into a single {@link DetectionResponseV2}. Returns {@code null} when neither side
   * produced anything.
   */
  private DetectionResponseV2 mergeToV2(
      DetectionResponse v1Aggregate, DetectionResponseV2 v2Aggregate) {
    if (v1Aggregate == null && v2Aggregate == null) {
      return null;
    }
    DetectionResponseV2 result =
        v2Aggregate != null
            ? v2Aggregate
            : DetectionResponseV2.builder().images(new HashMap<>()).build();
    var convertedV1 = detectionResponseV1ToV2Mapper.apply(v1Aggregate);
    if (convertedV1 != null && convertedV1.getImages() != null) {
      result.getImages().putAll(convertedV1.getImages());
    }
    return result;
  }

  private boolean hasVegetationModel(
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    return detectableObjectConfigurations.stream()
        .anyMatch(
            detectableObjectConfiguration ->
                detectableObjectTypeForVegetationModel().stream()
                    .map(detectableObjectType -> detectableObjectConfiguration.getObjectType())
                    .toList()
                    .contains(detectableObjectConfiguration.getObjectType()));
  }

  private DetectionPayloadV2 buildPayloadV2(
      String fileName, String base64ImgData, String base64MaskData, boolean vegetation) {
    var builder =
        DetectionPayloadV2.builder()
            .fileName(fileName)
            .base64ImgData(base64ImgData)
            .base64MaskData(base64MaskData);
    if (vegetation) {
      builder.vegetation(true);
    }
    return builder.build();
  }

  private DetectionPayload buildPayloadV1(
      String projectName,
      String fileName,
      String base64ImgData,
      String base64MaskData,
      boolean vegetation) {
    var payloadFileName = !fileName.contains(".") ? fileName + ".jpg" : fileName;
    var builder =
        DetectionPayload.builder()
            .projectName(projectName)
            .fileName(payloadFileName)
            .base64ImgData(base64ImgData)
            .base64MaskData(base64MaskData);
    if (vegetation) {
      builder.vegetation(true);
    }
    return builder.build();
  }

  @SneakyThrows
  private List<TileDetectorUrl> getApiUrls(
      List<DetectableObjectConfiguration> objectConfigurations) {
    List<TileDetectorUrl> tileDetectionApiUrls =
        om.readValue(tileObjectDetectorConf.getTileDetectionApiUrls(), new TypeReference<>() {});
    Map<String, TileDetectorUrl> urlsByUrl = new LinkedHashMap<>();
    for (var conf : objectConfigurations) {
      for (var url : tileDetectionApiUrls) {
        if (conf.getObjectType().equals(url.getObjectType())) {
          urlsByUrl.putIfAbsent(url.getUrl(), url);
        }
      }
    }
    var detectableTypes =
        tileDetectionApiUrls.stream().map(TileDetectorUrl::getObjectType).toList();
    var detectableTypesFromObjectConfiguration =
        objectConfigurations.stream().map(DetectableObjectConfiguration::getObjectType).toList();
    if (!new HashSet<>(detectableTypes).containsAll(detectableTypesFromObjectConfiguration)) {
      urlsByUrl.putIfAbsent(
          defaultDetectionApiUrl,
          TileDetectorUrl.builder().url(defaultDetectionApiUrl).version(V2).build());
    }
    return new ArrayList<>(urlsByUrl.values());
  }

  private static String insertSuffix(String path, String suffix, String extension) {
    if (path == null) {
      return null;
    }
    String normalizedExt = extension.startsWith(".") ? extension : "." + extension;

    int dotIndex = path.lastIndexOf('.');
    String base = (dotIndex < 0) ? path : path.substring(0, dotIndex);

    return base + suffix + normalizedExt;
  }

  private static String replaceSeparator(String path) {
    if (path == null) {
      return null;
    }
    return path.replace('/', '_');
  }

  private record DetectionApiCall(
      TileDetectorUrl apiUrl, DetectionResponse v1Response, DetectionResponseV2 v2Response) {}
}
