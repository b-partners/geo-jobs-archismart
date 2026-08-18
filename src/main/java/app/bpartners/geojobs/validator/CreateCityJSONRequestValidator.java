package app.bpartners.geojobs.validator;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationObjectType.BUILDING_ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_CONSTRAINED_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.USER_DEFINED_DELIMITATION;

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
  private static final int MAX_ROOFS_COUNT_SYNC = 1;
  private static final int MAX_ROOFS_COUNT_ASYNC = 5;
  private static final List<DelimitationObjectType> SUPPORTED_DELIMITATION_OBJECT_TYPE =
      List.of(BUILDING_ROOF, BUILDING_ROOF_SEGMENT_FACE);

  @Override
  public void accept(CreateCityJSONRequest request) {
    if (request.getDelimitations() == null || request.getDelimitations().isEmpty()) {
      throw new BadRequestException(
          "CityJSONRequest.delimitations is mandatory and cannot be empty");
    }

    if (request.getDelimitations().size() > MAX_ROOFS_COUNT_ASYNC) {
      throw new BadRequestException(
          "Requests with more than "
              + MAX_ROOFS_COUNT_ASYNC
              + " delimitations are not supported yet.");
    }
  }

  public void accept(ThreeDRequest request) {
    accept(request, true);
  }

  public void accept(ThreeDRequest request, boolean isAsync) {
    if (request.getDelimitations() == null || request.getDelimitations().isEmpty()) {
      throw new BadRequestException(
          "CityJSONRequest.delimitations is mandatory and cannot be empty");
    }

    var maxRoofs = isAsync ? MAX_ROOFS_COUNT_ASYNC : MAX_ROOFS_COUNT_SYNC;
    if (request.getDelimitations().size() > maxRoofs) {
      throw new BadRequestException(
          "Requests with more than " + maxRoofs + " delimitations are not supported yet.");
    }

    var delimitationType = request.getDelimitationType();
    if (PARCEL_CONSTRAINED_DELIMITATION.equals(delimitationType)) {
      throw new NotImplementedException(
          "PARCEL_CONSTRAINED_DELIMITATION delimitationType is not supported yet, only"
              + " PARCEL_FREE_DELIMITATION and USER_DEFINED_DELIMITATION are.");
    }
    // a Point carries no surface, so it cannot be the roof delimitation itself : looking the roof
    // up around it is what PARCEL_FREE_DELIMITATION is for.
    if (USER_DEFINED_DELIMITATION.equals(delimitationType) && hasPointDelimitation(request)) {
      throw new NotImplementedException(
          "USER_DEFINED_DELIMITATION delimitationType only supports Polygon and MultiPolygon"
              + " geometries, use PARCEL_FREE_DELIMITATION to provide a Point.");
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

  private static boolean hasPointDelimitation(ThreeDRequest request) {
    return request.getDelimitations().stream()
        .anyMatch(
            feature ->
                feature.getGeometry() != null
                    && feature.getGeometry().getActualInstance() instanceof Point);
  }
}
