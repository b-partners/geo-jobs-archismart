package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.service.BuildingFinder;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

class FeatureMapperTest {
  private static final double SELF_INTERSECTING_REPAIRED_AREA = 48.272;
  private static final double AREA_TOLERANCE = 0.001;

  private final String id = randomUUID().toString();
  BuildingFinder buildingFinderMock = mock(BuildingFinder.class);
  private final FeatureMapper subject =
      new FeatureMapper(new GeometryConverter(), buildingFinderMock);

  private Feature expectedFeature() {
    Feature feature = new Feature();
    var coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(
                        BigDecimal.valueOf(6.958009303660302),
                        BigDecimal.valueOf(43.543013820437459)),
                    List.of(
                        BigDecimal.valueOf(6.957965493371299),
                        BigDecimal.valueOf(43.543002082885863)),
                    List.of(
                        BigDecimal.valueOf(6.957822106008073),
                        BigDecimal.valueOf(43.543033084979541)),
                    List.of(
                        BigDecimal.valueOf(6.957796040201745),
                        BigDecimal.valueOf(43.543066366941567)),
                    List.of(
                        BigDecimal.valueOf(6.957877191721906),
                        BigDecimal.valueOf(43.543303862183095)),
                    List.of(
                        BigDecimal.valueOf(6.957988034043352),
                        BigDecimal.valueOf(43.54328420602328)),
                    List.of(
                        BigDecimal.valueOf(6.958082768541455),
                        BigDecimal.valueOf(43.543132354704881)),
                    List.of(
                        BigDecimal.valueOf(6.958009303660302),
                        BigDecimal.valueOf(43.543013820437459)))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(coordinates);
    multiPolygon.setType(MULTI_POLYGON);
    feature.setGeometry(new FeatureGeometry(multiPolygon));
    feature.getProperties().put("zoom", 20);
    feature.getProperties().put("id", id);
    return feature;
  }

  private Polygon expectedPolygon() {
    var start = new Coordinate(6.958009303660302, 43.543013820437459);
    return new GeometryFactory()
        .createPolygon(
            new Coordinate[] {
              start,
              new Coordinate(6.957965493371299, 43.543002082885863),
              new Coordinate(6.957822106008073, 43.543033084979541),
              new Coordinate(6.957796040201745, 43.543066366941567),
              new Coordinate(6.957877191721906, 43.543303862183095),
              new Coordinate(6.957988034043352, 43.54328420602328),
              new Coordinate(6.958082768541455, 43.543132354704881),
              start,
            });
  }

  @Test
  void feature_to_geo_tools_polygon_mapper_ok() {
    Polygon polygon = subject.toDomainPolygon(expectedFeature());

    assertEquals(expectedPolygon(), polygon);
  }

  @Test
  void geo_tools_polygon_to_rest_feature_mapper_ok() {
    Feature feature = subject.toRest(expectedPolygon(), 20, id);

    assertEquals(expectedFeature(), feature);
  }

  @Test
  void feature_to_geo_tools_polygon_mapper_with_null_zoom_ok() {
    var multipolygon = subject.domainToGeometry(toDomainFeature(expectedFeature()));

    assertEquals(expectedPolygon(), multipolygon.getGeometryN(0));
  }

  @Test
  void feature_to_domain_list_keeps_every_part_of_a_self_intersecting_ring() {
    var actual = subject.toDomainList(selfIntersectingFeature());

    assertEquals(4, actual.size(), "every repaired part is expected, not only the widest one");
    actual.forEach(polygon -> assertTrue(polygon.isValid()));
    actual.forEach(polygon -> assertTrue(polygon.getArea() > 0, "no part without any surface"));
    assertEquals(
        SELF_INTERSECTING_REPAIRED_AREA,
        actual.stream().mapToDouble(Polygon::getArea).sum(),
        AREA_TOLERANCE,
        "the whole repaired surface is expected to be kept");
  }

  @Test
  void polygon_feature_to_domain_list_keeps_every_part_of_a_self_intersecting_ring() {
    var feature = new Feature();
    feature.setGeometry(
        new FeatureGeometry(
            new app.bpartners.geojobs.endpoint.rest.model.Polygon()
                .coordinates(List.of(selfIntersectingRing()))));

    var actual = subject.toDomainList(feature);

    assertEquals(4, actual.size(), "every repaired part is expected, not only the widest one");
    assertEquals(
        SELF_INTERSECTING_REPAIRED_AREA,
        actual.stream().mapToDouble(Polygon::getArea).sum(),
        AREA_TOLERANCE,
        "the whole repaired surface is expected to be kept");
  }

  @Test
  void polygon_feature_to_domain_list_keeps_the_holes_as_holes() {
    var feature = new Feature();
    feature.setGeometry(
        new FeatureGeometry(
            new app.bpartners.geojobs.endpoint.rest.model.Polygon()
                .coordinates(List.of(square(0, 0, 10), square(2, 2, 6)))));

    var actual = subject.toDomainList(feature);

    assertEquals(1, actual.size(), "a hole is not a polygon of its own");
    assertEquals(1, actual.getFirst().getNumInteriorRing());
    assertEquals(64.0, actual.getFirst().getArea(), AREA_TOLERANCE, "the hole is subtracted");
  }

  @Test
  void multi_polygon_feature_to_domain_list_keeps_the_holes_as_holes() {
    var multiPolygon =
        new MultiPolygon().coordinates(List.of(List.of(square(0, 0, 10), square(2, 2, 6))));
    multiPolygon.setType(MULTI_POLYGON);
    var feature = new Feature();
    feature.setGeometry(new FeatureGeometry(multiPolygon));

    var actual = subject.toDomainList(feature);

    assertEquals(1, actual.size(), "a hole is not a polygon of its own");
    assertEquals(1, actual.getFirst().getNumInteriorRing());
    assertEquals(64.0, actual.getFirst().getArea(), AREA_TOLERANCE, "the hole is subtracted");
  }

  @Test
  void polygon_feature_to_domain_geometry_subtracts_the_holes() {
    var feature = new Feature();
    feature.setGeometry(
        new FeatureGeometry(
            new app.bpartners.geojobs.endpoint.rest.model.Polygon()
                .coordinates(List.of(square(0, 0, 10), square(2, 2, 6)))));

    var actual = subject.toDomainGeometry(feature);

    assertInstanceOf(Polygon.class, actual);
    assertEquals(1, ((Polygon) actual).getNumInteriorRing());
    assertEquals(64.0, actual.getArea(), AREA_TOLERANCE, "the hole is not detected surface");
  }

  @Test
  void multi_polygon_feature_to_domain_geometry_subtracts_the_holes() {
    var multiPolygon =
        new MultiPolygon().coordinates(List.of(List.of(square(0, 0, 10), square(2, 2, 6))));
    multiPolygon.setType(MULTI_POLYGON);
    var feature = new Feature();
    feature.setGeometry(new FeatureGeometry(multiPolygon));

    var actual = subject.toDomainGeometry(feature);

    assertEquals(64.0, actual.getArea(), AREA_TOLERANCE, "the hole is not detected surface");
  }

  @Test
  void polygon_feature_to_domain_geometry_repairs_the_self_intersection() {
    var feature = new Feature();
    feature.setGeometry(
        new FeatureGeometry(
            new app.bpartners.geojobs.endpoint.rest.model.Polygon()
                .coordinates(List.of(selfIntersectingRing()))));

    var actual = subject.toDomainGeometry(feature);

    assertTrue(actual.isValid(), "the domain geometry is expected to be valid");
    assertEquals(4, actual.getNumGeometries(), "every repaired part is expected");
    assertEquals(SELF_INTERSECTING_REPAIRED_AREA, actual.getArea(), AREA_TOLERANCE);
  }

  @Test
  void multi_polygon_feature_to_domain_geometry_repairs_the_self_intersection() {
    var actual = subject.toDomainGeometry(selfIntersectingFeature());

    assertTrue(actual.isValid(), "the domain geometry is expected to be valid");
    assertEquals(SELF_INTERSECTING_REPAIRED_AREA, actual.getArea(), AREA_TOLERANCE);
  }

  @Test
  void feature_to_domain_polygon_keeps_the_widest_repaired_part() {
    var actual = subject.toDomainPolygon(selfIntersectingFeature());

    assertTrue(actual.isValid(), "the domain polygon is expected to be valid");
    assertEquals(33.136, actual.getArea(), AREA_TOLERANCE, "the widest part is expected");
  }

  private List<List<BigDecimal>> square(double x, double y, double side) {
    return Stream.of(
            List.of(x, y),
            List.of(x + side, y),
            List.of(x + side, y + side),
            List.of(x, y + side),
            List.of(x, y))
        .map(
            point ->
                List.of(BigDecimal.valueOf(point.getFirst()), BigDecimal.valueOf(point.getLast())))
        .toList();
  }

  private Feature selfIntersectingFeature() {
    var multiPolygon = new MultiPolygon().coordinates(List.of(List.of(selfIntersectingRing())));
    multiPolygon.setType(MULTI_POLYGON);
    var feature = new Feature();
    feature.setGeometry(new FeatureGeometry(multiPolygon));
    return feature;
  }

  private List<List<BigDecimal>> selfIntersectingRing() {
    var ring =
        Stream.of(
                List.of(0, 0),
                List.of(6, 6),
                List.of(0, 6),
                List.of(6, 0),
                List.of(0, 0),
                List.of(30, 3),
                List.of(0, 3),
                List.of(0, 0))
            .map(
                point ->
                    List.of(
                        BigDecimal.valueOf(point.getFirst()), BigDecimal.valueOf(point.getLast())))
            .toList();
    return ring;
  }
}
