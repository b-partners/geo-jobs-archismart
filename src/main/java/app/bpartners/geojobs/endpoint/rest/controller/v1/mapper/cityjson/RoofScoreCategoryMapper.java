package app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.cityjson;

import app.bpartners.geojobs.endpoint.rest.model.RoofScoreCategory;
import app.bpartners.geojobs.model.geometry.area.Rate;
import org.springframework.stereotype.Component;

@Component
public class RoofScoreCategoryMapper {
  public RoofScoreCategory toRest(Rate domain) {
    return switch (domain) {
      case A -> RoofScoreCategory.A;
      case B -> RoofScoreCategory.B;
      case C -> RoofScoreCategory.C;
      case D -> RoofScoreCategory.D;
      case E -> RoofScoreCategory.E;
    };
  }
}
