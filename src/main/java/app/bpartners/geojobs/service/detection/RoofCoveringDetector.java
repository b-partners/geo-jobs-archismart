package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.service.detection.DetectionResponseV2.REGION_LABEL_PROPERTY;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.readFileToByteArray;

import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.RoofCoveringType;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.inference.InferenceApiClient;
import app.bpartners.geojobs.service.detection.inference.InferenceRequest;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import app.bpartners.geojobs.service.detection.inference.ResolvedModelCard;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class RoofCoveringDetector {
  private final ObjectMapper om;
  private final RestTemplate restTemplate;
  private final String roofCoveringDetectionApiUrl;
  private final CustomBucketComponent bucketComponent;
  private final InferenceApiClient inferenceApiClient;
  private final ModelCardResolver modelCardResolver;

  public RoofCoveringDetector(
      ObjectMapper om,
      RestTemplate restTemplate,
      @Value("${roof.covering.detection.api.url}") String roofCoveringDetectionApiUrl,
      CustomBucketComponent bucketComponent,
      InferenceApiClient inferenceApiClient,
      ModelCardResolver modelCardResolver) {
    this.om = om;
    this.restTemplate = restTemplate;
    this.roofCoveringDetectionApiUrl = roofCoveringDetectionApiUrl;
    this.bucketComponent = bucketComponent;
    this.inferenceApiClient = inferenceApiClient;
    this.modelCardResolver = modelCardResolver;
  }

  @SneakyThrows
  public RoofCoveringDetectionResponse apply(
      Tile tile, File maskFile, Double spatialResolutionInCm) {
    var tileImageFile =
        bucketComponent.download(
            bucketComponent.getBucketConf().getBucketName(), tile.getBucketPath());
    var imageBase64 = Base64.getEncoder().encodeToString(readFileToByteArray(tileImageFile));
    var maskBase64 =
        maskFile == null ? null : Base64.getEncoder().encodeToString(readFileToByteArray(maskFile));
    var resolvedModelCard = resolveModelCard(tile, tileImageFile, spatialResolutionInCm);
    if (resolvedModelCard != null) {
      return applyWithInferenceApi(
          resolvedModelCard, tileImageFile.getName(), imageBase64, maskBase64);
    }
    var coveringDetectionProjectName = randomUUID().toString();
    if (tile.getDetectionE2Id() == null) {
      log.info(
          "Actual project name for roof covering detection is {}", coveringDetectionProjectName);
    }
    return apply(
        new RoofCoveringDetectionPayload(
            imageBase64,
            maskBase64,
            tile.getBucketPath(),
            tile.getDetectionE2Id() == null
                ? coveringDetectionProjectName
                : tile.getDetectionE2Id()));
  }

  private ResolvedModelCard resolveModelCard(
      Tile tile, File tileImageFile, Double spatialResolutionInCm) {
    var tileSize = HttpApiTileObjectDetector.readImageSize(tileImageFile);
    return modelCardResolver
        .resolve(
            List.of(DetectableObjectConfiguration.builder().objectType(TOITURE_REVETEMENT).build()),
            spatialResolutionInCm,
            HttpApiTileObjectDetector.tileZoom(tile),
            modelCard -> HttpApiTileObjectDetector.supportsTileSize(modelCard, tileSize))
        .modelCards()
        .stream()
        .findFirst()
        .orElse(null);
  }

  private RoofCoveringDetectionResponse applyWithInferenceApi(
      ResolvedModelCard resolvedModelCard, String fileName, String imageBase64, String maskBase64) {
    var modelCard = resolvedModelCard.modelCard();
    if (modelCard.imagesRequired() != 1 || (modelCard.maskRequired() && maskBase64 == null)) {
      log.error(
          "Model card {} cannot be used for roof covering: imagesRequired={}, maskRequired={}, mask"
              + " provided={}",
          modelCard.modelCardId(),
          modelCard.imagesRequired(),
          modelCard.maskRequired(),
          maskBase64 != null);
      return null;
    }
    var request =
        InferenceRequest.builder()
            .modelCardId(modelCard.modelCardId())
            .image1(imageBase64)
            .mask1(modelCard.maskRequired() ? maskBase64 : null)
            .filename(fileName)
            .zoomLevel(resolvedModelCard.zoomLevel())
            .spatialResolution(resolvedModelCard.spatialResolution())
            .build();
    log.info("Attempting to call inference API for roof covering with {}", modelCard.modelCardId());
    try {
      return toRoofCoveringResponse(inferenceApiClient.infer(request));
    } catch (IllegalStateException e) {
      log.error(
          "Error while calling inference API for roof covering with model card {}: {}",
          modelCard.modelCardId(),
          e.getMessage());
      return null;
    }
  }

  private RoofCoveringDetectionResponse toRoofCoveringResponse(DetectionResponseV2 response) {
    if (response == null || response.getImages() == null) {
      return null;
    }
    var areaByCoating = new EnumMap<RoofCoveringType, Double>(RoofCoveringType.class);
    response.getImages().values().stream()
        .filter(image -> image.getRegions() != null)
        .flatMap(image -> image.getRegions().values().stream())
        .filter(region -> region.getShapeAttributes() != null)
        .forEach(
            region -> {
              var coating = toCoating(region.getRegionAttributes().get(REGION_LABEL_PROPERTY));
              if (coating != null) {
                areaByCoating.merge(coating, polygonArea(region.getShapeAttributes()), Double::sum);
              }
            });
    var sortedCoverings =
        areaByCoating.entrySet().stream()
            .sorted(Map.Entry.<RoofCoveringType, Double>comparingByValue().reversed())
            .map(entry -> new RoofCovering(entry.getKey(), Math.round(entry.getValue())))
            .toList();
    if (sortedCoverings.isEmpty()) {
      return null;
    }
    return new RoofCoveringDetectionResponse(
        sortedCoverings.getFirst(), sortedCoverings.size() > 1 ? sortedCoverings.get(1) : null);
  }

  private static RoofCoveringType toCoating(String label) {
    try {
      return RoofCoveringType.fromModelLabel(label);
    } catch (IllegalArgumentException e) {
      log.warn("Ignoring unknown roof covering label {}", label);
      return null;
    }
  }

  private static double polygonArea(DetectionResponseV2.ImageData.ShapeAttributes shape) {
    var xs = shape.getAllPointsX();
    var ys = shape.getAllPointsY();
    var doubledArea = 0.0;
    for (int i = 0; i < xs.size(); i++) {
      var next = (i + 1) % xs.size();
      doubledArea +=
          xs.get(i).doubleValue() * ys.get(next).doubleValue()
              - xs.get(next).doubleValue() * ys.get(i).doubleValue();
    }
    return Math.abs(doubledArea) / 2;
  }

  @SneakyThrows
  private RoofCoveringDetectionResponse apply(RoofCoveringDetectionPayload payload) {
    var headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    var requestBody = om.writeValueAsString(payload);
    var request = new HttpEntity<>(requestBody, headers);

    try {
      var uriBuilder =
          UriComponentsBuilder.fromUri(new URI(roofCoveringDetectionApiUrl + "/coatings"));
      var response =
          restTemplate.postForEntity(
              uriBuilder.toUriString(), request, RoofCoveringDetectionResponse.class);
      if (response.getStatusCode().value() == 200) {
        return response.getBody();
      }
    } catch (URISyntaxException | HttpStatusCodeException e) {
      log.error(
          "Error while calling API for roof covering detection {} with exception {}",
          roofCoveringDetectionApiUrl,
          e.getMessage());
    }
    return null;
  }

  public record RoofCoveringDetectionPayload(
      @JsonProperty("image_base64") String imageBase64,
      @JsonProperty("binary_mask_base64") String binaryMaskBase64,
      @JsonProperty("filename") String filename,
      @JsonProperty("projectname") String projectName) {}

  public record RoofCoveringDetectionResponse(RoofCovering primary, RoofCovering secondary) {}
}
