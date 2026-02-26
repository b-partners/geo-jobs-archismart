package app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationObjectType.BUILDING_ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_FREE_DELIMITATION;
import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CityJSONRequestMapper {
  private final BucketComponent bucketComponent;

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
        .status(CityJSONRequestStatusMapper.toRest(cityJSONRequest.getStatus()))
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
        .delimitationObjectType(BUILDING_ROOF)
        .delimitationType(PARCEL_FREE_DELIMITATION)
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

  public app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest createToDomain(
      String requestIdentifier,
      CreateCityJSONRequest createCityJSONRequest,
      String communityOwnerId) {
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
        .build();
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
            createCityJSONRequest.getDelimitationObjectType() == null
                ? BUILDING_ROOF
                : createCityJSONRequest.getDelimitationObjectType())
        .build();
  }
}
