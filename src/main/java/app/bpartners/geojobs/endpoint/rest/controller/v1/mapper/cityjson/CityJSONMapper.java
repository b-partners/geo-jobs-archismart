package app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.cityjson;

import app.bpartners.geojobs.endpoint.rest.model.CityJSON;
import app.bpartners.geojobs.endpoint.rest.model.CityJsonFileUrl;

public class CityJSONMapper {
  private CityJSONMapper() {}

  public static CityJSON toRest(
      app.bpartners.geojobs.repository.model.cityjson.CityJSON cityJSON, String fileUrl) {
    return new CityJSON().url(fileUrl).id(cityJSON.getId());
  }

  public static CityJsonFileUrl toRestCityJsonFileUrl(
      app.bpartners.geojobs.repository.model.cityjson.CityJSON cityJSON, String fileUrl) {
    return new CityJsonFileUrl().id(cityJSON.getId()).url(fileUrl);
  }
}
