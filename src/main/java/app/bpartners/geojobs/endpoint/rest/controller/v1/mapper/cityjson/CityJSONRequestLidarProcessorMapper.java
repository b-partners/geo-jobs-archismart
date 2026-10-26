package app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.cityjson;

import app.bpartners.geojobs.endpoint.rest.model.LidarProcessorType;

public class CityJSONRequestLidarProcessorMapper {
  private CityJSONRequestLidarProcessorMapper() {}

  public static LidarProcessorType toRest(
      app.bpartners.geojobs.model.lidar.LidarProcessorType processorType) {
    return switch (processorType) {
      case null -> null;
      case DEFAULT -> LidarProcessorType.DEFAULT;
      case THREE_D_BAG_ROOFER -> LidarProcessorType.THREE_D_BAG_ROOFER;
      case SAFE_MODE -> LidarProcessorType.SAFE_MODE;
    };
  }
}
