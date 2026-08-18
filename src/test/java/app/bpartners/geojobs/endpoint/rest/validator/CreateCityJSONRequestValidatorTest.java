package app.bpartners.geojobs.endpoint.rest.validator;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationObjectType.BUILDING_ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_CONSTRAINED_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_FREE_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.USER_DEFINED_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.endpoint.rest.model.ThreeDRequest;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.validator.CreateCityJSONRequestValidator;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateCityJSONRequestValidatorTest {
  CreateCityJSONRequestValidator subject = new CreateCityJSONRequestValidator();

  @Test
  void throws_not_implemented_exception_when_delimitation_type_is_parcel_constrained() {
    var actual =
        assertThrows(
            NotImplementedException.class,
            () -> subject.accept(request(PARCEL_CONSTRAINED_DELIMITATION, polygonFeature())));

    assertTrue(actual.getMessage().contains("PARCEL_CONSTRAINED_DELIMITATION"));
  }

  /** A Point carries no surface, so it cannot be the roof delimitation itself. */
  @Test
  void throws_not_implemented_exception_when_user_defined_delimitation_is_given_a_point() {
    var actual =
        assertThrows(
            NotImplementedException.class,
            () -> subject.accept(request(USER_DEFINED_DELIMITATION, pointFeature())));

    assertTrue(actual.getMessage().contains("USER_DEFINED_DELIMITATION"));
  }

  @Test
  void does_not_throw_when_user_defined_delimitation_is_given_a_polygon_or_multi_polygon() {
    assertDoesNotThrow(() -> subject.accept(request(USER_DEFINED_DELIMITATION, polygonFeature())));
    assertDoesNotThrow(
        () -> subject.accept(request(USER_DEFINED_DELIMITATION, multiPolygonFeature())));
  }

  @Test
  void does_not_throw_when_parcel_free_delimitation_is_given_a_point() {
    assertDoesNotThrow(() -> subject.accept(request(PARCEL_FREE_DELIMITATION, pointFeature())));
  }

  /** A null delimitationType defaults to PARCEL_FREE_DELIMITATION downstream. */
  @Test
  void does_not_throw_when_delimitation_type_is_not_given() {
    assertDoesNotThrow(() -> subject.accept(request(null, pointFeature())));
    assertDoesNotThrow(() -> subject.accept(request(null, polygonFeature())));
  }

  private static ThreeDRequest request(
      app.bpartners.geojobs.endpoint.rest.model.DelimitationType delimitationType,
      Feature delimitation) {
    return new ThreeDRequest()
        .delimitations(List.of(delimitation))
        .delimitationObjectType(BUILDING_ROOF)
        .delimitationType(delimitationType);
  }

  private static Feature pointFeature() {
    var point =
        new Point()
            .type(Point.TypeEnum.POINT)
            .coordinates(List.of(BigDecimal.valueOf(6.87), BigDecimal.valueOf(47.68)));
    return new Feature().type(FEATURE).geometry(new FeatureGeometry(point));
  }

  private static Feature polygonFeature() {
    var polygon =
        new Polygon().type(Polygon.TypeEnum.POLYGON).coordinates(List.of(List.of(coordinate())));
    return new Feature().type(FEATURE).geometry(new FeatureGeometry(polygon));
  }

  private static Feature multiPolygonFeature() {
    var multiPolygon =
        new MultiPolygon()
            .type(MultiPolygon.TypeEnum.MULTI_POLYGON)
            .coordinates(List.of(List.of(List.of(coordinate()))));
    return new Feature().type(FEATURE).geometry(new FeatureGeometry(multiPolygon));
  }

  private static List<BigDecimal> coordinate() {
    return List.of(BigDecimal.valueOf(6.87), BigDecimal.valueOf(47.68));
  }
}
