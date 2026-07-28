package app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson;

import app.bpartners.geojobs.endpoint.rest.model.DelimitationObjectType;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType;

public class CityJSONDelimitationObjectTypeMapper {
  private CityJSONDelimitationObjectTypeMapper() {}

  public static CityJSONDelimitationObjectType fromRestDelimitationObjectType(
      DelimitationObjectType delimitationObjectType) {
    return switch (delimitationObjectType) {
      case null -> CityJSONDelimitationObjectType.BUILDING_ROOF;
      case BUILDING_ROOF -> CityJSONDelimitationObjectType.BUILDING_ROOF;
      case BUILDING_ROOF_SEGMENT_FACE -> CityJSONDelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE;
    };
  }

  public static DelimitationObjectType toRestDelimitationObjectType(
      CityJSONDelimitationObjectType delimitationObjectType) {
    return switch (delimitationObjectType) {
      case null -> DelimitationObjectType.BUILDING_ROOF;
      case BUILDING_ROOF -> DelimitationObjectType.BUILDING_ROOF;
      case BUILDING_ROOF_SEGMENT_FACE -> DelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE;
    };
  }
}
