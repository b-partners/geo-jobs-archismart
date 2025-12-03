package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
class GeometrySquareMeterAreaTest {
  GeometrySquareMeterArea subject = new GeometrySquareMeterArea();

  @SneakyThrows
  @Test
  void geometry_area_pcrs() {
    var feature = new ObjectMapper().readValue(featureFromZonePCRS(), Feature.class);
    var geometry =
        new GeometryConverter(null, null)
            .apply(feature.getGeometry().getMultiPolygon().getCoordinates());

    var actual = subject.apply(geometry);

    assertEquals(Math.round(10495.227197824406), Math.round(actual));
  }

  @Test
  void change_polygon_crs() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(639000, 6833000),
          new Coordinate(639000, 6834000),
          new Coordinate(638000, 6834000),
          new Coordinate(638000, 6833000),
          new Coordinate(639000, 6833000)
        };
    var roofGeometry = geometryFactory.createPolygon(coordinates);

    var expectedCoords =
        new Coordinate[] {
          new Coordinate(2.172788043543686, 48.59432975513147),
          new Coordinate(2.172645960661405, 48.60332446371527),
          new Coordinate(2.1590837968932357, 48.60322944328981),
          new Coordinate(2.159228208479135, 48.594234750323594),
          new Coordinate(2.172788043543686, 48.59432975513147)
        };

    var expectedP = geometryFactory.createPolygon(expectedCoords);

    var projected =
        subject.project(
            roofGeometry, GeometrySquareMeterArea.LAMBERT_93, GeometrySquareMeterArea.WGS84);

    assertTrue(projected.equalsExact(expectedP, 1e-13));
  }

  @SneakyThrows
  @Test
  void geometry_area_tours() {
    var feature = new ObjectMapper().readValue(featureFromZoneTours(), Feature.class);
    var geometry =
        new GeometryConverter(null, null)
            .apply(feature.getGeometry().getMultiPolygon().getCoordinates());

    var actual = subject.apply(geometry);

    assertEquals(Math.round(10197.745005498364), Math.round(actual));
  }

  private String featureFromZoneTours() {
    return """
           {
                                "type": "Feature",
                                "properties": {},
                                "geometry": {
                                    "type": "MultiPolygon",
                                    "coordinates": [
                                        [
                                            [
                                                [
                                                    0.684264757502766,
                                                    47.389443733426205
                                                ],
                                                [
                                                    0.684565164912434,
                                                    47.38865926459548
                                                ],
                                                [
                                                    0.683342077601643,
                                                    47.388325135436794
                                                ],
                                                [
                                                    0.6828807376510815,
                                                    47.38925488088589
                                                ],
                                                [
                                                    0.684264757502766,
                                                    47.389443733426205
                                                ]
                                            ]
                                        ]
                                    ]
                                }
                            }""";
  }

  private String featureFromZonePCRS() {
    return """
           {
                       "type": "Feature",
                       "geometry": {
                           "coordinates": [
                               [
                                   [
                                       [
                                           0.6754183664424431,
                                           47.38125018586146
                                       ],
                                       [
                                           0.6754183664424431,
                                           47.38013583016112
                                       ],
                                       [
                                           0.6765420562064719,
                                           47.38013583016112
                                       ],
                                       [
                                           0.6765420562064719,
                                           47.38125018586146
                                       ],
                                       [
                                           0.6754183664424431,
                                           47.38125018586146
                                       ]
                                   ]
                               ]
                           ],
                           "type": "MultiPolygon"
                       },
                       "properties": {}
                   }""";
  }

  @SneakyThrows
  @Test
  void compute_3x3_tiles_area() {
    var geometryConverter = new GeometryConverter(null, null);
    var featureDomainList =
        new ObjectMapper()
            .readValue(
                domainFeatureListFor3x3Tiles(),
                new TypeReference<List<app.bpartners.geojobs.repository.model.Feature>>() {});
    var tile3x3MultiPolygon =
        featureDomainList.stream()
            .map(
                feature ->
                    geometryConverter.readGeometryFromString(
                        feature.getGeometry().getActualInstanceStringValue()))
            .map(
                geometry -> {
                  if (geometry instanceof Polygon polygon) {
                    return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
                  }
                  return null;
                })
            .filter(Objects::nonNull)
            .reduce(unifyMultiPolygon())
            .orElseThrow();

    var actual = subject.apply(tile3x3MultiPolygon);

    assertEquals(6209.0, Math.round(actual));
  }

  private String domainFeatureListFor3x3Tiles() {
    return """
[
  {
    "id": null,
    "zoom": null,
    "geometry": {
      "geometryType": "Polygon",
      "actualInstanceStringValue": "{\\"coordinates\\":[[[-1.6736984252929688,46.546819304239754],[-1.6736984252929688,46.54705542793464],[-1.6733551025390625,46.54705542793464],[-1.6733551025390625,46.546819304239754],[-1.6736984252929688,46.546819304239754]]],\\"type\\":\\"Polygon\\"}"
    },
    "properties": {}
  },
  {
    "id": null,
    "zoom": null,
    "geometry": {
      "geometryType": "Polygon",
      "actualInstanceStringValue": "{\\"coordinates\\":[[[-1.6736984252929688,46.546583179517754],[-1.6736984252929688,46.546819304239754],[-1.6733551025390625,46.546819304239754],[-1.6733551025390625,46.546583179517754],[-1.6736984252929688,46.546583179517754]]],\\"type\\":\\"Polygon\\"}"
    },
    "properties": {}
  },
  {
    "id": null,
    "zoom": null,
    "geometry": {
      "geometryType": "Polygon",
      "actualInstanceStringValue": "{\\"coordinates\\":[[[-1.6736984252929688,46.54634705376863],[-1.6736984252929688,46.546583179517754],[-1.6733551025390625,46.546583179517754],[-1.6733551025390625,46.54634705376863],[-1.6736984252929688,46.54634705376863]]],\\"type\\":\\"Polygon\\"}"
    },
    "properties": {}
  },
  {
    "id": null,
    "zoom": null,
    "geometry": {
      "geometryType": "Polygon",
      "actualInstanceStringValue": "{\\"coordinates\\":[[[-1.6733551025390625,46.546819304239754],[-1.6733551025390625,46.54705542793464],[-1.6730117797851562,46.54705542793464],[-1.6730117797851562,46.546819304239754],[-1.6733551025390625,46.546819304239754]]],\\"type\\":\\"Polygon\\"}"
    },
    "properties": {}
  },
  {
    "id": null,
    "zoom": null,
    "geometry": {
      "geometryType": "Polygon",
      "actualInstanceStringValue": "{\\"coordinates\\":[[[-1.6733551025390625,46.546583179517754],[-1.6733551025390625,46.546819304239754],[-1.6730117797851562,46.546819304239754],[-1.6730117797851562,46.546583179517754],[-1.6733551025390625,46.546583179517754]]],\\"type\\":\\"Polygon\\"}"
    },
    "properties": {}
  },
  {
    "id": null,
    "zoom": null,
    "geometry": {
      "geometryType": "Polygon",
      "actualInstanceStringValue": "{\\"coordinates\\":[[[-1.6733551025390625,46.54634705376863],[-1.6733551025390625,46.546583179517754],[-1.6730117797851562,46.546583179517754],[-1.6730117797851562,46.54634705376863],[-1.6733551025390625,46.54634705376863]]],\\"type\\":\\"Polygon\\"}"
    },
    "properties": {}
  },
  {
    "id": null,
    "zoom": null,
    "geometry": {
      "geometryType": "Polygon",
      "actualInstanceStringValue": "{\\"coordinates\\":[[[-1.6730117797851562,46.546819304239754],[-1.6730117797851562,46.54705542793464],[-1.67266845703125,46.54705542793464],[-1.67266845703125,46.546819304239754],[-1.6730117797851562,46.546819304239754]]],\\"type\\":\\"Polygon\\"}"
    },
    "properties": {}
  },
  {
    "id": null,
    "zoom": null,
    "geometry": {
      "geometryType": "Polygon",
      "actualInstanceStringValue": "{\\"coordinates\\":[[[-1.6730117797851562,46.546583179517754],[-1.6730117797851562,46.546819304239754],[-1.67266845703125,46.546819304239754],[-1.67266845703125,46.546583179517754],[-1.6730117797851562,46.546583179517754]]],\\"type\\":\\"Polygon\\"}"
    },
    "properties": {}
  },
  {
    "id": null,
    "zoom": null,
    "geometry": {
      "geometryType": "Polygon",
      "actualInstanceStringValue": "{\\"coordinates\\":[[[-1.6730117797851562,46.54634705376863],[-1.6730117797851562,46.546583179517754],[-1.67266845703125,46.546583179517754],[-1.67266845703125,46.54634705376863],[-1.6730117797851562,46.54634705376863]]],\\"type\\":\\"Polygon\\"}"
    },
    "properties": {}
  }
]""";
  }
}
