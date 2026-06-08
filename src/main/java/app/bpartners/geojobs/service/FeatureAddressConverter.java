package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;

import app.bpartners.geojobs.model.DelimitationObjectType;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

// TODO: rename to make both address and point generic
@Component
public class FeatureAddressConverter
    implements BiFunction<String, DelimitationObjectType, Feature> {
  private final GeometryConverter geometryConverter;
  private final BuildingFinder buildingFinder;
  private final GeoCodeService geoCodeService;

  public FeatureAddressConverter(
      GeometryConverter geometryConverter,
      BuildingFinder buildingFinder,
      GeoCodeService geoCodeService) {
    this.geometryConverter = geometryConverter;
    this.buildingFinder = buildingFinder;
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
    var nearestRoofMultiPolygon =
        buildingFinder.getBuildingMultiPolygon(
            List.of(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)));
    var properties = new HashMap<String, Object>();
    if (address != null) {
      properties.put("address", address);
    }
    return geometryConverter.toFeature(
        null, HOUSES_0.getZoomLevel(), properties, nearestRoofMultiPolygon);
  }
}
