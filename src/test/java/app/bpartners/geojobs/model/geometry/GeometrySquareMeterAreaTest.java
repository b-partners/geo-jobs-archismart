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

  @SneakyThrows
  @Test
  void geometry_building_around_10_000_pcrs() {
    var feature =
        new ObjectMapper().readValue(featureAroundTenThousandSquareMeterZone(), Feature.class);
    var geometry =
        new GeometryConverter(null, null)
            .apply(feature.getGeometry().getMultiPolygon().getCoordinates());

    var actual = subject.apply(geometry);

    assertEquals(7816, Math.round(actual));
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

  private String featureAroundTenThousandSquareMeterZone() {
    return """
           {
                          "type": "Feature",
                          "geometry": {
                            "coordinates": [
                              [
                                [
                                  [
                                    -0.254572415020339,
                                    46.64418118115884
                                  ],
                                  [
                                    -0.254538410687296,
                                    46.64415961719834
                                  ],
                                  [
                                    -0.254459238159678,
                                    46.64421413487083
                                  ],
                                  [
                                    -0.254539919407377,
                                    46.64427222313812
                                  ],
                                  [
                                    -0.25466967502369,
                                    46.6443631627319
                                  ],
                                  [
                                    -0.254728324425942,
                                    46.644402951649475
                                  ],
                                  [
                                    -0.254728015113394,
                                    46.64446334018993
                                  ],
                                  [
                                    -0.254230054888112,
                                    46.644801010191635
                                  ],
                                  [
                                    -0.254209598267396,
                                    46.64478717209705
                                  ],
                                  [
                                    -0.254020976171097,
                                    46.64491509042753
                                  ],
                                  [
                                    -0.253986810008862,
                                    46.64489082732568
                                  ],
                                  [
                                    -0.253876216653206,
                                    46.64496696415559
                                  ],
                                  [
                                    -0.253807938283265,
                                    46.64491933751635
                                  ],
                                  [
                                    -0.253802928442516,
                                    46.64492308453143
                                  ],
                                  [
                                    -0.253795198240708,
                                    46.64492510641171
                                  ],
                                  [
                                    -0.253787414178611,
                                    46.644926228631
                                  ],
                                  [
                                    -0.253778269936264,
                                    46.64492648828227
                                  ],
                                  [
                                    -0.253770324295316,
                                    46.64492491151894
                                  ],
                                  [
                                    -0.253763631115785,
                                    46.64492239800175
                                  ],
                                  [
                                    -0.253756830218465,
                                    46.64491808516318
                                  ],
                                  [
                                    -0.253751335642279,
                                    46.64491373523152
                                  ],
                                  [
                                    -0.253748399847819,
                                    46.64490841145383
                                  ],
                                  [
                                    -0.253746770373736,
                                    46.64490305058345
                                  ],
                                  [
                                    -0.253747699680284,
                                    46.64489671586726
                                  ],
                                  [
                                    -0.253749989165171,
                                    46.64489124371894
                                  ],
                                  [
                                    -0.253754945147495,
                                    46.64488659704565
                                  ],
                                  [
                                    -0.253758702528081,
                                    46.6448837867859
                                  ],
                                  [
                                    -0.25372328404235,
                                    46.644860460358615
                                  ],
                                  [
                                    -0.253743431117895,
                                    46.64484727163175
                                  ],
                                  [
                                    -0.253326855770033,
                                    46.64455539851133
                                  ],
                                  [
                                    -0.253223052671756,
                                    46.64448264563542
                                  ],
                                  [
                                    -0.253076893256036,
                                    46.64438003247115
                                  ],
                                  [
                                    -0.2532202,
                                    46.64428459999997
                                  ],
                                  [
                                    -0.2533837,
                                    46.64440339999999
                                  ],
                                  [
                                    -0.2539759,
                                    46.64400509999997
                                  ],
                                  [
                                    -0.2539038,
                                    46.64395489999998
                                  ],
                                  [
                                    -0.2541337,
                                    46.64379959999997
                                  ],
                                  [
                                    -0.2541539,
                                    46.6438139
                                  ],
                                  [
                                    -0.254625468964418,
                                    46.64414319036593
                                  ],
                                  [
                                    -0.254572415020339,
                                    46.64418118115884
                                  ]
                                ]
                              ]
                            ],
                            "type": "MultiPolygon"
                          },
                          "properties": {
                            "address": "1 Av. François Mitterrand, 79200 Parthenay, France"
                          }
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
