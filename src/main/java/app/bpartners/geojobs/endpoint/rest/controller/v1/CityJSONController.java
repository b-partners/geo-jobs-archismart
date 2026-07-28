package app.bpartners.geojobs.endpoint.rest.controller.v1;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;

import app.bpartners.geojobs.endpoint.rest.V1RestController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.cityjson.CityJSONRequestMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CityJSONRequestValidator;
import app.bpartners.geojobs.model.lidar.LidarProcessorType;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.CityJSONRequestService;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import app.bpartners.geojobs.validator.CreateCityJSONRequestValidator;
import app.bpartners.geojobs.validator.ThreeDAddressesRequestValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@V1RestController
@RequiredArgsConstructor
public class CityJSONController {
  private final CityJSONRequestMapper cityJSONRequestMapper;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final AuthProvider authProvider;
  private final CityJSONRequestService cityJSONRequestService;
  private final CityJSONRequestValidator cityJSONRequestValidator;
  private final CreateCityJSONRequestValidator createCityJSONRequestValidator;
  private final ThreeDAddressesRequestValidator threeDAddressesRequestValidator;
  private final FeatureAddressConverter featureAddressConverter;

  @GetMapping("/3d/{id}")
  public ThreeDResponseStatus getRequested3DFileById(@PathVariable(name = "id") String requestId) {
    var communityOwnerId = getCommunityAuthorizationId();

    return cityJSONRequestMapper.toRestThreeDResponseStatus(
        cityJSONRequestService.getByIdAndCommunityOwnerId(requestId, communityOwnerId));
  }

  @PostMapping("/3d/{id}")
  public ThreeDResponseStatus request3DFileOnDelimitations(
      @RequestBody ThreeDRequest threeDRequest,
      @PathVariable(name = "id") String requestIdentifier) {
    var communityOwnerId = getCommunityAuthorizationId();
    createCityJSONRequestValidator.accept(threeDRequest);
    cityJSONRequestValidator.accept(requestIdentifier, communityOwnerId);

    var toProcess =
        cityJSONRequestMapper.createToDomain(requestIdentifier, threeDRequest, communityOwnerId);

    return cityJSONRequestMapper.toRestThreeDResponseStatus(
        cityJSONRequestService.process(toProcess));
  }

  @PostMapping("/3d/{id}/sync")
  public ThreeDResponseStatus request3DFileOnDelimitationsSync(
      @RequestBody ThreeDRequest threeDRequest,
      @PathVariable(name = "id") String requestIdentifier) {
    var communityOwnerId = getCommunityAuthorizationId();
    createCityJSONRequestValidator.accept(threeDRequest, false);
    cityJSONRequestValidator.accept(requestIdentifier, communityOwnerId);

    var toProcess =
        cityJSONRequestMapper.createToDomain(requestIdentifier, threeDRequest, communityOwnerId);

    return cityJSONRequestMapper.toRestThreeDResponseStatus(
        cityJSONRequestService.processSync(toProcess));
  }

  @PostMapping("/3d/{id}/addresses")
  public ThreeDResponseStatus request3DFileOnAddresses(
      @RequestBody ThreeDAddressesRequest threeDRequest,
      @PathVariable(name = "id") String requestIdentifier) {
    threeDAddressesRequestValidator.accept(threeDRequest);
    var communityOwnerId = getCommunityAuthorizationId();
    cityJSONRequestValidator.accept(requestIdentifier, communityOwnerId);
    if (threeDRequest.getAddresses().size() == 1) {
      var convertedAddressesToDelimitations =
          threeDRequest.getAddresses().stream()
              .map(AddressFullText::getFullText)
              .map(addressValue -> featureAddressConverter.apply(addressValue, BUILDING))
              .map(FeatureMapper::toRestFeature)
              .toList();

      var request = new ThreeDRequest().delimitations(convertedAddressesToDelimitations);
      var toProcess =
          cityJSONRequestMapper.createToDomain(requestIdentifier, request, communityOwnerId);
      return cityJSONRequestMapper.toRestThreeDResponseStatus(
          cityJSONRequestService.process(toProcess));
    }
    var lidarProcessorType = threeDRequest.getLidarProcessorType();
    var domainLidarProcessorType =
        lidarProcessorType == null ? null : LidarProcessorType.valueOf(lidarProcessorType.name());
    var savedRequest =
        cityJSONRequestService.processAddressRequest(
            requestIdentifier,
            threeDRequest.getAddresses().stream().map(AddressFullText::getFullText).toList(),
            communityOwnerId,
            domainLidarProcessorType);

    return cityJSONRequestMapper.toRestThreeDResponseStatus(savedRequest);
  }

  @PutMapping("/city-jsons/{id}/process")
  public CityJSONRequest processCityJSONRequest(
      @RequestBody CreateCityJSONRequest createCityJSONRequest,
      @PathVariable(name = "id") String requestIdentifier) {
    createCityJSONRequestValidator.accept(createCityJSONRequest);

    var communityOwnerId = getCommunityAuthorizationId();
    var toProcess =
        cityJSONRequestMapper.createToDomainFromDeprecatedDTO(
            requestIdentifier, createCityJSONRequest, communityOwnerId);

    return cityJSONRequestMapper.toRest(cityJSONRequestService.oldProcess(toProcess));
  }

  @GetMapping("/city-jsons/{id}")
  public CityJSONRequest getById(@PathVariable(name = "id") String requestId) {
    var communityOwnerId = getCommunityAuthorizationId();

    return cityJSONRequestMapper.toRest(
        cityJSONRequestService.getByIdAndCommunityOwnerId(requestId, communityOwnerId));
  }

  private String getCommunityAuthorizationId() {
    var communityAuthorization =
        communityAuthorizationRepository.findByApiKey(authProvider.getPrincipal().getPassword());
    return communityAuthorization.map(CommunityAuthorization::getId).orElse(null);
  }
}
