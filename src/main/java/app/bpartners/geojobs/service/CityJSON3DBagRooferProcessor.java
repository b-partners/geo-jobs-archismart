package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.service.cityjson.model.object.CityJsonIO;
import app.bpartners.geojobs.service.cityjson.texture.CityJsonTextureComputer;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.lidar.api.LidarApiFacade;
import app.bpartners.geojobs.service.roofer3dbag.Roofer3DBagApiClient;
import app.bpartners.geojobs.service.roofer3dbag.model.CityJsonGenerationRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityJSON3DBagRooferProcessor implements Function<CityJSONRequest, List<CityJSON>> {
  private static final String JSONL_EXTENSION = ".jsonl";
  private static final String GEOJSON_EXTENSION = ".geojson";
  private static final String JSON_EXTENSION = ".json";
  private final BucketComponent bucketComponent;
  private final FeatureMapper featureMapper;
  private final LidarApiFacade lidarApiFacade;
  private final Roofer3DBagApiClient roofer3DBagApiClient;
  private final FileWriter fileWriter;
  private final CoordinateTransformer coordinateTransformer;
  private final GeometryConverter geometryConverter;
  private final CityJsonTextureComputer textureComputer;

  @Override
  public List<CityJSON> apply(CityJSONRequest request) {
    var delimitationFeatureGeoJsonFileURL = retrieveGeometriesWithPresignedURL(request);
    var cityJsonGenerationResponses =
        delimitationFeatureGeoJsonFileURL.entrySet().stream()
            .map(
                entry -> {
                  var geoJsonBuildingPresignedUrl = entry.getKey();

                  return roofer3DBagApiClient.generateCityJson(
                      CityJsonGenerationRequest.builder()
                          .geoJsonBuildingPresignedUrl(geoJsonBuildingPresignedUrl)
                          .lidarPresignedUrls(entry.getValue().stream().toList())
                          .build(),
                      request.getComplexityFactor());
                })
            .toList();

    return cityJsonGenerationResponses.stream()
        .map(
            cityJsonGenerationResponse -> {
              var bucketFileKey = randomUUID() + JSON_EXTENSION;
              File cityJSONConvertedInJsonExtension;
              try {
                URL rooferCityJsonURL;
                rooferCityJsonURL = new URI(cityJsonGenerationResponse.getCityJsonUrl()).toURL();
                File originalCityJSONFile =
                    File.createTempFile(randomUUID().toString(), JSONL_EXTENSION);
                try (InputStream in = rooferCityJsonURL.openStream()) {
                  Files.copy(
                      in, originalCityJSONFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                var cityJSONFileWithAdditionalProperties =
                    File.createTempFile(bucketFileKey, JSONL_EXTENSION);

                var computedCityJSON = CityJsonIO.computeAdditionalProperties(originalCityJSONFile);

                CityJsonIO.write(computedCityJSON, cityJSONFileWithAdditionalProperties.toPath());

                var tmpFile = File.createTempFile(bucketFileKey, JSON_EXTENSION);

                cityJSONConvertedInJsonExtension =
                    CityJsonIO.convertCityJsonSeqToCityJson(
                        cityJSONFileWithAdditionalProperties.toPath(), tmpFile.toPath());

              } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
              }

              var texturedCityJSON =
                  textureComputer.applyTexture(request, cityJSONConvertedInJsonExtension);

              bucketComponent.upload(texturedCityJSON, bucketFileKey);

              var cityJsonId = randomUUID().toString();
              return CityJSON.builder()
                  .id(cityJsonId)
                  .request(request)
                  .s3FileKey(bucketFileKey)
                  .build();
            })
        .toList();
  }

  private Map<String, Set<String>> retrieveGeometriesWithPresignedURL(CityJSONRequest request) {
    return request.getRequestDelimitations().stream()
        .map(
            feature -> {
              try {
                var presignURL = getGeoJsonBuildingPresignedURL(feature);
                var presignURLString = presignURL.toString();
                var uniqueLidarFilesUrls = getUniqueLidarFilesUrls(feature);
                log.info("Presigned URL for building: " + presignURLString);
                log.info("Lidar files URLs: " + uniqueLidarFilesUrls);
                return Map.of(presignURLString, uniqueLidarFilesUrls);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .flatMap(map -> map.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Set<String> getUniqueLidarFilesUrls(Feature feature) {
    var geometry = featureMapper.domainToGeometryWithMultipolygonHandler(feature);
    var geometries = Collections.singleton(geometry);
    return lidarApiFacade.getUniqueLidarFilesUrls(geometries).keySet();
  }

  private URL getGeoJsonBuildingPresignedURL(Feature feature) throws IOException {
    var tmpGeoJsonBucketKey = randomUUID() + GEOJSON_EXTENSION;
    var multiPolygon = getMultiPolygon(feature);
    var lambert93Coordinates = convertWgs84ToLambert93Coordinates(multiPolygon);
    var geoJson =
        new GeoJson(List.of(new GeoJson.GeoFeature(feature.getProperties(), lambert93Coordinates)));

    var tmpGeoJsonFile =
        fileWriter.write(
            geoJson.getStringValue().getBytes(UTF_8), createTempDirectory(), tmpGeoJsonBucketKey);

    bucketComponent.upload(tmpGeoJsonFile, tmpGeoJsonBucketKey);

    return bucketComponent.presign(tmpGeoJsonBucketKey, Duration.ofHours(1L));
  }

  private MultiPolygon getMultiPolygon(Feature feature) {
    var restFeature = toRestFeature(feature);
    var actualInstance = restFeature.getGeometry().getActualInstance();
    if (actualInstance instanceof MultiPolygon m) {
      return m;
    } else if (actualInstance instanceof Polygon p) {
      return new MultiPolygon().type(MULTI_POLYGON).coordinates(List.of(p.getCoordinates()));
    }
    throw new NotImplementedException(
        "Unsupported geometry type for validation: " + actualInstance);
  }

  private MultiPolygon convertWgs84ToLambert93Coordinates(MultiPolygon multiPolygon) {
    var convertedCoordinates =
        coordinateTransformer.apply(geometryConverter.apply(multiPolygon.getCoordinates()));
    if (convertedCoordinates
        instanceof org.locationtech.jts.geom.MultiPolygon multiPolygonConverted) {
      return geometryConverter.restMultiPolygonFromJts(multiPolygonConverted);
    }
    throw new NotImplementedException(
        "Unable to convert coordinates to Lambert93 for multiPolygon " + multiPolygon);
  }
}
