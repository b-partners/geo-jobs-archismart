package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.threed.model.DelimitationType.PANEL_BY_PANEL_DELIMITATION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.service.cityjson.texture.CityJsonTextureComputer;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.threed.ThreedApiClient;
import app.bpartners.geojobs.service.threed.exception.ThreedApiException;
import app.bpartners.geojobs.service.threed.model.CreateLrgFromFeatureFileUrl;
import app.bpartners.geojobs.service.threed.model.DelimitationType;
import app.bpartners.geojobs.service.threed.model.GenerationStatus;
import app.bpartners.geojobs.service.threed.model.Lrg;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityJSONThreedProcessor implements Function<CityJSONRequest, List<CityJSON>> {
  private static final String JSON_EXTENSION = ".json";
  private static final String GEOJSON_EXTENSION = ".geojson";

  private final FileWriter fileWriter;
  private final BucketComponent bucketComponent;
  private final CityJsonTextureComputer textureComputer;
  private final ThreedApiClient threedApiClient;
  private final CityJSONDownloader cityJSONDownloader;
  private final CommunityAuthorizationRepository communityRepository;

  @Component
  public static class CityJSONDownloader {
    public File download(String url) {
      try {
        var cityjsonFileUrl = new URI(url).toURL();
        var file = File.createTempFile(randomUUID().toString(), JSON_EXTENSION);

        try (var in = cityjsonFileUrl.openStream()) {
          Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return file;

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private String getApiKey(String communityId) {
    var community = communityRepository.findById(communityId);
    if (community.isEmpty())
      throw new IllegalArgumentException("No community was found for id = " + communityId);
    var apiKey = community.get().getMostRecentApiKey();
    if (apiKey == null)
      throw new IllegalArgumentException("No API key was found for community id = " + communityId);
    return apiKey.getKeyValue();
  }

  @Override
  public List<CityJSON> apply(CityJSONRequest request) {
    return applyWithDiagnostics(request).cityJsons();
  }

  /**
   * Same generation as {@link #apply}, plus the message and warnings threed reported for any
   * DEGRADED building, so the caller can surface them to its own API clients instead of discarding
   * them.
   */
  public ThreedGenerationResult applyWithDiagnostics(CityJSONRequest request) {
    var apiKey = getApiKey(request.getCommunityOwnerId());
    var id = request.getId();
    var delimitationType = getDelimitationType(request);
    var delimitationFeatureGeoJsonFileURL = retrieveGeometriesWithPresignedURL(request);

    var cityJsonGenerationResponses =
        delimitationFeatureGeoJsonFileURL.stream()
            .map(
                buildingUrl ->
                    threedApiClient.generate(
                        id,
                        CreateLrgFromFeatureFileUrl.builder()
                            .featureFileUrl(buildingUrl)
                            .delimitationType(delimitationType)
                            .build(),
                        apiKey))
            .toList();

    cityJsonGenerationResponses.forEach(CityJSONThreedProcessor::checkGenerationStatus);

    var cityJsons =
        cityJsonGenerationResponses.stream()
            .map(response -> downloadAndStore(request, response))
            .toList();

    var degraded =
        cityJsonGenerationResponses.stream()
            .filter(response -> response.getStatus() == GenerationStatus.DEGRADED)
            .toList();
    var degradedMessage =
        degraded.isEmpty()
            ? null
            : degraded.stream()
                .map(Lrg::getMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    var warnings =
        degraded.stream()
            .map(Lrg::getWarnings)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .distinct()
            .toList();

    return new ThreedGenerationResult(cityJsons, degradedMessage, warnings);
  }

  private CityJSON downloadAndStore(CityJSONRequest request, Lrg response) {
    var bucketFileKey = randomUUID() + JSON_EXTENSION;
    var cityjsonFile = cityJSONDownloader.download(response.getFileUrl());
    cityjsonFile.deleteOnExit();

    var textured = textureComputer.applyTexture(request, cityjsonFile);

    bucketComponent.upload(textured, bucketFileKey);

    var cityJsonId = randomUUID().toString();
    return CityJSON.builder().id(cityJsonId).request(request).s3FileKey(bucketFileKey).build();
  }

  /**
   * threed can report a DEGRADED generation (e.g. some roof panels had no lidar coverage and were
   * rendered as ground only): the fileUrl is still usable, only log it. NO_LIDAR and FAILED have no
   * usable fileUrl to download, so they must not be treated as success; NO_LIDAR is reported the
   * same way the former HTTP 422 was, so callers keep mapping it to UNAVAILABLE.
   */
  static void checkGenerationStatus(Lrg response) {
    switch (response.getStatus()) {
      case NORMAL -> {}
      case DEGRADED -> log.warn("Threed generation partially succeeded: {}", response.getMessage());
      case NO_LIDAR ->
          throw new ThreedApiException(
              HttpStatus.OK,
              "NO_LIDAR_DATA_FOUND",
              response.getMessage(),
              "Threed generation found no lidar coverage: " + response.getMessage());
      case FAILED ->
          throw new ThreedApiException("Threed generation failed: " + response.getMessage(), null);
    }
  }

  private Set<String> retrieveGeometriesWithPresignedURL(CityJSONRequest request) {
    return request.getRequestDelimitations().stream()
        .map(
            feature -> {
              var presignURL = getGeoJsonBuildingPresignedURL(feature);
              var presignURLString = presignURL.toString();
              log.info("Presigned URL for building: {}", presignURLString);
              return presignURLString;
            })
        .collect(toSet());
  }

  private URL getGeoJsonBuildingPresignedURL(Feature feature) {
    var tmpGeoJsonBucketKey = randomUUID() + GEOJSON_EXTENSION;
    var multiPolygon = getMultiPolygon(feature);
    var geoJson = new GeoJson(new GeoJson.GeoFeature(feature.getProperties(), multiPolygon));

    var tmpGeoJsonFile =
        fileWriter.write(
            geoJson.getStringValue().getBytes(UTF_8), createTempDirectory(), tmpGeoJsonBucketKey);

    bucketComponent.upload(tmpGeoJsonFile, tmpGeoJsonBucketKey);

    return bucketComponent.presign(tmpGeoJsonBucketKey, Duration.ofHours(1L));
  }

  private MultiPolygon getMultiPolygon(Feature feature) {
    var restFeature = requireNonNull(toRestFeature(feature));
    var actualInstance = requireNonNull(restFeature.getGeometry()).getActualInstance();
    if (actualInstance instanceof MultiPolygon m) {
      return m;
    } else if (actualInstance instanceof Polygon p) {
      return new MultiPolygon()
          .type(MULTI_POLYGON)
          .coordinates(List.of(requireNonNull(p.getCoordinates())));
    }
    throw new NotImplementedException(
        "Unsupported geometry type for validation: " + actualInstance);
  }

  private DelimitationType getDelimitationType(CityJSONRequest request) {
    var delimitationObjectType = request.getDelimitationObjectType();
    return switch (delimitationObjectType) {
      case BUILDING_ROOF_SEGMENT_FACE -> PANEL_BY_PANEL_DELIMITATION;
      case null, default -> DelimitationType.ENTIRE_ROOF_DELIMITATION;
    };
  }
}
