package app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.USER_DEFINED_DELIMITATION;
import static app.bpartners.geojobs.model.lidar.LidarProcessorType.THREE_D_BAG_ROOFER;
import static java.math.RoundingMode.HALF_UP;
import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.lidar.LidarProcessorType;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONTexture;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CityJSONRequestMapper {
  private final BucketComponent bucketComponent;
  private final CityJSONTextureMapper textureMapper;

  public CityJSONRequest toRest(
      app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest cityJSONRequest) {
    var restDelimitations =
        cityJSONRequest.getDelimitations().stream().map(FeatureMapper::toRestFeature).toList();

    List<CityJSON> cityJsons =
        cityJSONRequest.getCityJsons() == null ? List.of() : cityJSONRequest.getCityJsons();
    var restCityJsons =
        cityJsons.parallelStream()
            .map(
                cityJson -> {
                  var fileUrl = bucketComponent.presign(cityJson.getS3FileKey());
                  return CityJSONMapper.toRest(cityJson, fileUrl);
                })
            .toList();

    return new CityJSONRequest()
        .id(cityJSONRequest.getId())
        .delimitations(restDelimitations)
        .delimitationObjectType(
            CityJSONDelimitationObjectTypeMapper.toRestDelimitationObjectType(
                cityJSONRequest.getDelimitationObjectType()))
        .status(CityJSONRequestStatusMapper.toRest(cityJSONRequest.getStatus()))
        .delimitationType(cityJSONRequest.getDelimitationType())
        .threeDTextureInfo(null)
        .cityJsons(restCityJsons);
  }

  public ThreeDResponseStatus toRestThreeDResponseStatus(
      app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest cityJSONRequest) {
    var restDelimitations =
        cityJSONRequest.getDelimitations() == null
            ? null
            : cityJSONRequest.getDelimitations().stream()
                .map(FeatureMapper::toRestFeature)
                .toList();

    List<CityJSON> cityJsons =
        cityJSONRequest.getCityJsons() == null ? null : cityJSONRequest.getCityJsons();
    var restCityJsons =
        cityJsons == null
            ? null
            : cityJsons.parallelStream()
                .map(
                    cityJson -> {
                      var fileUrl = bucketComponent.presign(cityJson.getS3FileKey());
                      return CityJSONMapper.toRestCityJsonFileUrl(cityJson, fileUrl);
                    })
                .toList();
    var step = toRestStep(cityJSONRequest);
    return new ThreeDResponseStatus()
        .id(cityJSONRequest.getId())
        .delimitations(restDelimitations)
        .step(step)
        .status(CityJSONRequestStatusMapper.toGenericStatusRest(cityJSONRequest.getStatus()))
        .delimitationObjectType(
            CityJSONDelimitationObjectTypeMapper.toRestDelimitationObjectType(
                cityJSONRequest.getDelimitationObjectType()))
        .delimitationType(cityJSONRequest.getDelimitationType())
        .complexityFactor(
            cityJSONRequest.getComplexityFactor() == null
                ? null
                : BigDecimal.valueOf(cityJSONRequest.getComplexityFactor()).setScale(2, HALF_UP))
        .knn(cityJSONRequest.getKnn())
        .cityJsonFileUrls(restCityJsons);
  }

  private ThreeDRequestStep toRestStep(
      app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest cityJSONRequest) {
    if (cityJSONRequest.getStep() == null) {
      return null;
    }
    switch (cityJSONRequest.getStep()) {
      case REQUEST_ACCEPTED -> {
        return ThreeDRequestStep.REQUEST_ACCEPTED;
      }
      case POINTS_CLOUD_PRE_PROCESSING -> {
        return ThreeDRequestStep.POINTS_CLOUD_PRE_PROCESSING;
      }
      case GEOMETRY_CONSTRUCTION -> {
        return ThreeDRequestStep.GEOMETRY_CONSTRUCTION;
      }
      case POST_PROCESSING -> {
        return ThreeDRequestStep.POST_PROCESSING;
      }
      default ->
          throw new IllegalStateException(
              "Unexpected 3d request step value: " + cityJSONRequest.getStep());
    }
  }

  private List<CityJSONTexture> getDomainTexture(
      ThreeDTextureInfo texture,
      app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest request) {
    if (texture == null) {
      return List.of();
    }
    // TODO: handle multiple textures if possible
    return List.of(textureMapper.toDomain(texture, request));
  }

  public app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest
      createToDomainFromDeprecatedDTO(
          String requestIdentifier,
          CreateCityJSONRequest createCityJSONRequest,
          String communityOwnerId) {
    List<Feature> delimitations =
        createCityJSONRequest.getDelimitations() == null
            ? List.of()
            : createCityJSONRequest.getDelimitations();
    var domainDelimitations = delimitations.stream().map(FeatureMapper::toDomainFeature).toList();
    var domain =
        app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest.builder()
            .id(requestIdentifier)
            .creationDatetime(now())
            .communityOwnerId(communityOwnerId)
            .delimitations(domainDelimitations)
            .lidarProcessorType(THREE_D_BAG_ROOFER)
            .delimitationObjectType(
                CityJSONDelimitationObjectTypeMapper.fromRestDelimitationObjectType(
                    createCityJSONRequest.getDelimitationObjectType()))
            .delimitationType(USER_DEFINED_DELIMITATION)
            .build();
    var textures = getDomainTexture(createCityJSONRequest.getThreeDTextureInfo(), domain);
    return domain.toBuilder().textures(textures).build();
  }

  public app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest createToDomain(
      String requestIdentifier, ThreeDRequest createCityJSONRequest, String communityOwnerId) {
    List<Feature> delimitations =
        createCityJSONRequest.getDelimitations() == null
            ? List.of()
            : createCityJSONRequest.getDelimitations();
    var domainDelimitations = delimitations.stream().map(FeatureMapper::toDomainFeature).toList();

    return app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest.builder()
        .id(requestIdentifier)
        .creationDatetime(now())
        .communityOwnerId(communityOwnerId)
        .delimitations(domainDelimitations)
        .delimitationObjectType(
            CityJSONDelimitationObjectTypeMapper.fromRestDelimitationObjectType(
                createCityJSONRequest.getDelimitationObjectType()))
        .delimitationType(createCityJSONRequest.getDelimitationType())
        .lidarProcessorType(
            createCityJSONRequest.getLidarProcessorType() == null
                ? null
                : LidarProcessorType.valueOf(createCityJSONRequest.getLidarProcessorType().name()))
        .complexityFactor(
            createCityJSONRequest.getComplexityFactor() == null
                ? null
                : createCityJSONRequest.getComplexityFactor().floatValue())
        .knn(createCityJSONRequest.getKnn())
        .build();
  }
}
