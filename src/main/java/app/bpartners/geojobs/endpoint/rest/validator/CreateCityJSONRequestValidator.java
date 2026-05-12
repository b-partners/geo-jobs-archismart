package app.bpartners.geojobs.endpoint.rest.validator;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationObjectType.BUILDING_ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_FREE_DELIMITATION;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateCityJSONRequestValidator implements Consumer<CreateCityJSONRequest> {
  private static final int MAX_ROOFS_COUNT = 5;
  private static final List<DelimitationObjectType> SUPPORTED_DELIMITATION_OBJECT_TYPE =
      List.of(BUILDING_ROOF, BUILDING_ROOF_SEGMENT_FACE);

  @Override
  public void accept(CreateCityJSONRequest request) {
    if (request.getDelimitations() == null || request.getDelimitations().isEmpty()) {
      throw new BadRequestException(
          "CityJSONRequest.delimitations is mandatory and cannot be empty");
    }

    if (request.getDelimitations().size() > MAX_ROOFS_COUNT) {
      throw new BadRequestException(
          "Requests with more than " + MAX_ROOFS_COUNT + " delimitations are not supported yet.");
    }
  }

  public void accept(ThreeDRequest request) {
    if (request.getDelimitations() == null || request.getDelimitations().isEmpty()) {
      throw new BadRequestException(
          "CityJSONRequest.delimitations is mandatory and cannot be empty");
    }

    if (request.getDelimitations().size() > MAX_ROOFS_COUNT) {
      throw new BadRequestException(
          "Requests with more than " + MAX_ROOFS_COUNT + " delimitations are not supported yet.");
    }

    if (request.getDelimitationType() != null
        && !PARCEL_FREE_DELIMITATION.equals(request.getDelimitationType())) {
      throw new NotImplementedException(
          "Only PARCEL_FREE_DELIMITATION delimitationType supported for now, otherwise actual is "
              + request.getDelimitationType());
    }

    var delimitationObjectType = request.getDelimitationObjectType();
    if (delimitationObjectType != null
        && !SUPPORTED_DELIMITATION_OBJECT_TYPE.contains(delimitationObjectType)) {
      throw new NotImplementedException(
          "Only BUILDING_ROOF and BUILDING_ROOF_SEGMENT_FACE delimitationObjectType supported for"
              + " now, otherwise actual is "
              + request.getDelimitationObjectType());
    }

    if (request.getDelimitations().size() > 1
        && !(request.getDelimitations().stream()
                .allMatch(
                    feature ->
                        feature.getGeometry() != null
                            && feature.getGeometry().getActualInstance() instanceof Point)
            || request.getDelimitations().stream()
                .allMatch(
                    feature ->
                        feature.getGeometry() != null
                            && feature.getGeometry().getActualInstance() instanceof Polygon)
            || request.getDelimitations().stream()
                .allMatch(
                    feature ->
                        feature.getGeometry() != null
                            && feature.getGeometry().getActualInstance()
                                instanceof MultiPolygon))) {
      throw new NotImplementedException(
          "Provided delimitations must be either all Points or all Polygons or all MultiPolygons.");
    }
  }
}
