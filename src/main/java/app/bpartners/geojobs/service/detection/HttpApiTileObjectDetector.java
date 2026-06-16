package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper.detectableObjectTypeForVegetationModel;
import static app.bpartners.geojobs.service.detection.DetectionApiVersion.V2;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.*;
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
  private final ObjectMapper om;
  private final CustomBucketComponent customBucketComponent;
  private final String defaultDetectionApiUrl;
  private final TileObjectDetectorConf tileObjectDetectorConf;
  private final DetectionResponseAggregator detectionResponseAggregator;
  private final DetectionResponseAggregatorV1 detectionResponseAggregatorV1;
  private final DetectionResponseV1ToV2Mapper detectionResponseV1ToV2Mapper;
  private final BucketComponent bucketComponent;

  @SneakyThrows
  public HttpApiTileObjectDetector(
      ObjectMapper om,
      CustomBucketComponent bucketComponent,
      @Value("${tile.detection.api.url}") String defaultApiUrl,
      TileObjectDetectorConf tileObjectDetectorConf,
      DetectionResponseAggregator detectionResponseAggregator,
      DetectionResponseAggregatorV1 detectionResponseAggregatorV1,
      DetectionResponseV1ToV2Mapper detectionResponseV1ToV2Mapper,
      BucketComponent bucketComponent1) {
    this.om = om;
    this.customBucketComponent = bucketComponent;
    this.defaultDetectionApiUrl = defaultApiUrl;
    this.tileObjectDetectorConf = tileObjectDetectorConf;
    this.detectionResponseAggregator = detectionResponseAggregator;
    this.detectionResponseAggregatorV1 = detectionResponseAggregatorV1;
    this.detectionResponseV1ToV2Mapper = detectionResponseV1ToV2Mapper;
    this.bucketComponent = bucketComponent1;
  }

  @SneakyThrows
  @Override
  public DetectionResponseV2 apply(
      TileDetectionTask tileDetectionTask,
      File mask,
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    Tile tile = tileDetectionTask.getTile();
    if (tile == null) {
      return null;
    }
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);

    var tileImageBucketPath = tile.getBucketPath();
    var maskBucketKey = "mask_" + tileImageBucketPath;
    if (mask != null) {
      bucketComponent.upload(mask, maskBucketKey);
    }
    var tileImageFile =
        customBucketComponent.download(
            customBucketComponent.getBucketConf().getBucketName(), tileImageBucketPath);
    var tileCoordinates = tile.getCoordinates();
    var coordinatesDescription =
        tileCoordinates.getZ() + "_" + tileCoordinates.getX() + "_" + tileCoordinates.getY();
    log.info(
        "{} coordinates original image : {} ",
        coordinatesDescription,
        customBucketComponent.presign(
            tileImageBucketPath,
            Duration.ofHours(1L),
            Optional.of(coordinatesDescription + ".jpeg")));
    if (mask != null) {
      log.info(
          "{} coordinates mask : {} ",
          coordinatesDescription,
          customBucketComponent.presign(
              maskBucketKey,
              Duration.ofHours(1L),
              Optional.of("mask_" + coordinatesDescription + ".png")));
    }
    String base64ImgData = Base64.getEncoder().encodeToString(readFileToByteArray(tileImageFile));
    String base64MaskData =
        mask == null ? null : Base64.getEncoder().encodeToString(readFileToByteArray(mask));
    boolean vegetation = hasVegetationModel(detectableObjectConfigurations);

    var requestV2 =
        new HttpEntity<>(
            om.writeValueAsString(
                buildPayloadV2(tileImageFile.getName(), base64ImgData, base64MaskData, vegetation)),
            headers);
    var requestV1 =
        new HttpEntity<>(
            om.writeValueAsString(
                buildPayloadV1(
                    tileDetectionTask.getJobId(),
                    tileImageFile.getName(),
                    base64ImgData,
                    base64MaskData,
                    vegetation)),
            headers);

    var detectionApiUrls = getApiUrls(detectableObjectConfigurations);
    var v2Responses = new ArrayList<DetectionResponseAggregator.DetectionResponseUrl>();
    var v1Responses = new ArrayList<DetectionResponseAggregatorV1.DetectionResponseUrl>();
    for (var apiUrl : detectionApiUrls) {
      if (apiUrl.getVersion() == DetectionApiVersion.V1) {
        var body = callApi(restTemplate, requestV1, apiUrl.getUrl(), DetectionResponse.class);
        if (body != null) {
          v1Responses.add(
              new DetectionResponseAggregatorV1.DetectionResponseUrl(body, apiUrl.getUrl()));
        }
      } else {
        var body = callApi(restTemplate, requestV2, apiUrl.getUrl(), DetectionResponseV2.class);
        if (body != null) {
          v2Responses.add(
              new DetectionResponseAggregator.DetectionResponseUrl(body, apiUrl.getUrl()));
        }
      }
    }

    return mergeToV2(
        detectionResponseAggregatorV1.apply(v1Responses),
        detectionResponseAggregator.apply(v2Responses));
  }

  private <T> T callApi(
      RestTemplate restTemplate, HttpEntity<String> request, String apiUrl, Class<T> responseType) {
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
    var builder =
        DetectionPayload.builder()
            .projectName(projectName)
            .fileName(fileName)
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
}
