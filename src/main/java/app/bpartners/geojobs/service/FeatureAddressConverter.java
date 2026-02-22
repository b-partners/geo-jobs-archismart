package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.model.DelimitationObjectType;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.dashboard.AreaPictureApi;
import app.bpartners.geojobs.service.dashboard.mapper.AreaPictureDetailsMapper;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

// TODO: rename to make both address and point generic
@Component
public class FeatureAddressConverter
    implements BiFunction<String, DelimitationObjectType, Feature> {
  private final AreaPictureApi areaPictureApi;
  private final AreaPictureDetailsMapper areaPictureDetailsMapper;
  private final String adminApiKey;
  private final GeometryConverter geometryConverter;

  public FeatureAddressConverter(
      AreaPictureApi areaPictureApi,
      AreaPictureDetailsMapper areaPictureDetailsMapper,
      @Value("${admin.api.key}") String adminApiKey,
      GeometryConverter geometryConverter) {
    this.areaPictureApi = areaPictureApi;
    this.areaPictureDetailsMapper = areaPictureDetailsMapper;
    this.adminApiKey = adminApiKey;
    this.geometryConverter = geometryConverter;
  }

  @Override
  public Feature apply(String address, DelimitationObjectType delimitationObjectType) {
    if (BUILDING.equals(delimitationObjectType)) {
      var areaPictureId = randomUUID().toString();
      var crupdateAreaPictureDetails =
          areaPictureDetailsMapper.toCrupdateAreaPictureDetails(address);
      Double longitude = null, latitude = null;
      try {
        var areaPictureDetails =
            areaPictureApi.crupdateAreaPictureDetails(
                areaPictureId, crupdateAreaPictureDetails, adminApiKey);
        longitude = areaPictureDetails.currentGeoPosition().longitude();
        latitude = areaPictureDetails.currentGeoPosition().latitude();
      } catch (HttpClientErrorException | HttpServerErrorException e) {
        throwsExceptionOnAddress(address);
      }
      if (longitude == null) {
        throwsExceptionOnAddress(address);
      }
      return apply(address, longitude, latitude);
    }
    throw new NotImplementedException(
        "Unable to convert address to Feature for delimitationObjectType "
            + delimitationObjectType);
  }

  // TODO: include delimitationObjectType when generalized
  public Feature apply(String address, Double longitude, Double latitude) {
    var nearestRoofMultiPolygon =
        geometryConverter.retrieveNearestRoofMultiPolygon(
            List.of(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)));
    var properties = new HashMap<String, Object>();
    if (address != null) {
      properties.put("address", address);
    }
    return geometryConverter.toFeature(
        null, HOUSES_0.getZoomLevel(), properties, nearestRoofMultiPolygon);
  }

  private void throwsExceptionOnAddress(String address) {
    throw new ApiException(
        SERVER_EXCEPTION,
        "Unable to convert address to GPS coordinate (longitude, latitude) : " + address);
  }
}
