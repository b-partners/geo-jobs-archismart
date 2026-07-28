package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;

import app.bpartners.geojobs.model.DelimitationObjectType;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import java.math.BigDecimal;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

// TODO: rename to make both address and point generic
@Component
public class FeatureAddressConverter
    implements BiFunction<String, DelimitationObjectType, Feature> {
  private final GeoCodeService geoCodeService;

  public FeatureAddressConverter(GeoCodeService geoCodeService) {
    this.geoCodeService = geoCodeService;
  }

  @Override
  public Feature apply(String address, DelimitationObjectType delimitationObjectType) {
    if (BUILDING.equals(delimitationObjectType)) {
      return geoCodeService.geocode(address);
    }
    throw new NotImplementedException(
        "Unable to convert address to Feature for delimitationObjectType "
            + delimitationObjectType);
  }

  // TODO: include delimitationObjectType when generalized
  public Feature apply(String address, Double longitude, Double latitude) {
    return geoCodeService.geocode(
        address, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude));
  }
}
