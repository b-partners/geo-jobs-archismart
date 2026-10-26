package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
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
import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import app.bpartners.geojobs.service.lidar.api.WalloniaBoundaryChecker;
import app.bpartners.geojobs.service.roofer3dbag.Roofer3DBagApiClient;
import app.bpartners.geojobs.service.roofer3dbag.model.CityJsonGenerationRequest;
import app.bpartners.geojobs.service.roofer3dbag.model.CityJsonGenerationResponse;
import app.bpartners.geojobs.service.roofer3dbag.validator.Roofer3DBagCityJSONValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityJSON3DBagRooferProcessor implements Function<CityJSONRequest, List<CityJSON>> {
  private static final String JSONL_EXTENSION = ".jsonl";
  private static final String GEOJSON_EXTENSION = ".geojson";
  private static final String JSON_EXTENSION = ".json";
  private static final String OGC_CRS_URL_PREFIX = "https://www.opengis.net/def/crs/EPSG/0/";
  private final BucketComponent bucketComponent;
  private final FeatureMapper featureMapper;
  private final LidarApiFacade lidarApiFacade;
  private final Roofer3DBagApiClient roofer3DBagApiClient;
  private final FileWriter fileWriter;
  private final CoordinateTransformer coordinateTransformer;
  private final GeometryConverter geometryConverter;
  private final CityJsonTextureComputer textureComputer;
  private final SwissBoundaryChecker swissBoundaryChecker;
  private final WalloniaBoundaryChecker walloniaBoundaryChecker;
  private final Roofer3DBagCityJSONValidator cityJSONValidator;
  private final ObjectMapper objectMapper;

  @Override
  public List<CityJSON> apply(CityJSONRequest request) {
    var buildingUploads = retrieveGeometriesWithPresignedURL(request);
    var complexityFactor = request.getComplexityFactor();
    var knn = request.getKnn();

    var generationResults =
        buildingUploads.stream()
            .map(
                upload -> {
                  var response =
                      roofer3DBagApiClient.generateCityJson(
                          CityJsonGenerationRequest.builder()
                              .geoJsonBuildingPresignedUrl(upload.presignedUrl())
                              .lidarPresignedUrls(upload.lidarUrls().stream().toList())
                              .build(),
                          complexityFactor,
                          knn);
                  return new RooferGenerationResult(response, upload.epsgCode());
                })
            .toList();

    return generationResults.stream()
        .map(
            generationResult -> {
              var cityJsonGenerationResponse = generationResult.response();
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

              // the roofer 3D-BAG service does not reliably tag metadata.referenceSystem for
              // every CRS it's fed (observed wrong for Belgian Lambert 2008) - override it with
              // the CRS we actually sent, since that's the one source of truth we control.
              overrideReferenceSystem(
                  cityJSONConvertedInJsonExtension, generationResult.epsgCode());

              cityJSONValidator.accept(cityJSONConvertedInJsonExtension);

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

  private void overrideReferenceSystem(File cityJsonFile, String epsgCode) {
    try {
      var json = (ObjectNode) objectMapper.readTree(cityJsonFile);
      var metadata =
          json.has("metadata")
              ? (ObjectNode) json.get("metadata")
              : objectMapper.createObjectNode();
      metadata.put("referenceSystem", OGC_CRS_URL_PREFIX + epsgCode.substring("EPSG:".length()));
      json.set("metadata", metadata);
      objectMapper.writeValue(cityJsonFile, json);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<BuildingGeoJsonUpload> retrieveGeometriesWithPresignedURL(CityJSONRequest request) {
    return request.getRequestDelimitations().stream()
        .map(
            feature -> {
              try {
                var presignedGeoJson = getGeoJsonBuildingPresignedURL(feature);
                var uniqueLidarFilesUrls = getUniqueLidarFilesUrls(feature);
                log.info("Presigned URL for building: " + presignedGeoJson.url());
                log.info("Lidar files URLs: " + uniqueLidarFilesUrls);
                return new BuildingGeoJsonUpload(
                    presignedGeoJson.url().toString(),
                    uniqueLidarFilesUrls,
                    presignedGeoJson.epsgCode());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }

  private Set<String> getUniqueLidarFilesUrls(Feature feature) {
    var geometry = featureMapper.domainToGeometryWithMultipolygonHandler(feature);
    var geometries = Collections.singleton(geometry);
    return lidarApiFacade.getUniqueLidarFilesUrls(geometries).filesUrls().keySet();
  }

  private PresignedGeoJson getGeoJsonBuildingPresignedURL(Feature feature) throws IOException {
    var tmpGeoJsonBucketKey = randomUUID() + GEOJSON_EXTENSION;
    var multiPolygon = getMultiPolygon(feature);
    var geometry = featureMapper.domainToGeometryWithMultipolygonHandler(feature);
    MultiPolygon coordinates;
    String epsgCode;
    if (swissBoundaryChecker.isGeometryInSwiss(geometry)) {
      coordinates = convertWgs84ToSwissCoordinates(multiPolygon);
      epsgCode = "EPSG:2056";
    } else if (walloniaBoundaryChecker.isGeometryInWallonia(geometry)) {
      coordinates = convertWgs84ToBelgiqueCoordinates(multiPolygon);
      epsgCode = "EPSG:3812";
    } else {
      coordinates = convertWgs84ToLambert93Coordinates(multiPolygon);
      epsgCode = "EPSG:2154";
    }

    var geoJson =
        new GeoJson(List.of(new GeoJson.GeoFeature(feature.getProperties(), coordinates)));

    var tmpGeoJsonFile =
        fileWriter.write(
            geoJson.getStringValue().getBytes(UTF_8), createTempDirectory(), tmpGeoJsonBucketKey);

    bucketComponent.upload(tmpGeoJsonFile, tmpGeoJsonBucketKey);

    var presignedUrl = bucketComponent.presign(tmpGeoJsonBucketKey, Duration.ofHours(1L));
    return new PresignedGeoJson(presignedUrl, epsgCode);
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

  private MultiPolygon convertCoordinates(
      MultiPolygon multiPolygon, Function<Geometry, Geometry> transformer) {
    var convertedCoordinates =
        transformer.apply(geometryConverter.apply(multiPolygon.getCoordinates()));
    if (convertedCoordinates
        instanceof org.locationtech.jts.geom.MultiPolygon multiPolygonConverted) {
      return geometryConverter.restMultiPolygonFromJts(multiPolygonConverted);
    }
    throw new NotImplementedException(
        "Unable to convert coordinates for multiPolygon " + multiPolygon);
  }

  private MultiPolygon convertWgs84ToSwissCoordinates(MultiPolygon multiPolygon) {
    return convertCoordinates(multiPolygon, coordinateTransformer::convertToSwissCoordinates);
  }

  private MultiPolygon convertWgs84ToLambert93Coordinates(MultiPolygon multiPolygon) {
    return convertCoordinates(multiPolygon, coordinateTransformer::apply);
  }

  private MultiPolygon convertWgs84ToBelgiqueCoordinates(MultiPolygon multiPolygon) {
    return convertCoordinates(multiPolygon, coordinateTransformer::convertToBelgiqueCRS);
  }

  private record PresignedGeoJson(URL url, String epsgCode) {}

  private record BuildingGeoJsonUpload(
      String presignedUrl, Set<String> lidarUrls, String epsgCode) {}

  private record RooferGenerationResult(CityJsonGenerationResponse response, String epsgCode) {}
}
