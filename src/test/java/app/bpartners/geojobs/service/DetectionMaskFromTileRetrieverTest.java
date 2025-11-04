package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;

class DetectionMaskFromTileRetrieverTest {
  GeometryPixelProjector geometryPixelProjector = mock(GeometryPixelProjector.class);
  GeometryConverter geometryConverterMock = mock();
  TileCoordinatesPolygonIntersection tilePolygonIntersectionMock =
      new TileCoordinatesPolygonIntersection(geometryPixelProjector, geometryConverterMock);
  DetectionMaskCreator maskCreatorMock = mock();
  DetectionMaskFromTileRetriever subject =
      new DetectionMaskFromTileRetriever(maskCreatorMock, tilePolygonIntersectionMock);

  @BeforeEach
  void setUp() {
    when(geometryConverterMock.writeGeometryAsString(any())).thenCallRealMethod();
    when(geometryConverterMock.readGeometryFromString(any())).thenCallRealMethod();
  }

  @Test
  void retrieve_mask_from_tile_with_point_intersection_ignored() {
    var tileMock = mock(Tile.class);
    var roofMultiPolygonMock = roofMultiPolygonForPoint();
    var multiPolygonFromTile = multiPolygonFromTileForPoint();
    int xTile = 523561;
    int yTile = 370292;
    int zoom = 20;
    var tileCoordinates = new TileCoordinates().x(xTile).y(yTile).z(zoom);
    when(tileMock.getCoordinates()).thenReturn(tileCoordinates);
    when(geometryConverterMock.getMultiPolygonFromTile(eq(xTile), eq(yTile), eq(zoom)))
        .thenReturn(multiPolygonFromTile);
    when(geometryPixelProjector.toPixels(
            any(Geometry.class), anyInt(), anyInt(), anyInt(), anyInt()))
        .thenCallRealMethod();
    var fileMaskMock = mock(File.class);
    when(maskCreatorMock.apply(any())).thenReturn(fileMaskMock);

    var actual = subject.apply(tileMock, roofMultiPolygonMock);

    assertEquals(fileMaskMock, actual);
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(maskCreatorMock).apply(listCaptor.capture());
    var intersectedCoordinates = (List<BigDecimal>) listCaptor.getAllValues().getFirst();
    assertTrue(intersectedCoordinates.isEmpty());
  }

  @Test
  @Disabled("TODO: flaky test")
  void retrieve_mask_from_tile_with_polygon_intersection() {
    var tileMock = mock(Tile.class);
    var roofMultiPolygon = roofMultiPolygonForMultiPolygonIntersection();
    int xTile = 523562;
    int yTile = 370292;
    int zoom = 20;
    var tileCoordinates = new TileCoordinates().x(xTile).y(yTile).z(zoom);
    when(tileMock.getCoordinates()).thenReturn(tileCoordinates);
    when(geometryConverterMock.getMultiPolygonFromTile(eq(xTile), eq(yTile), eq(zoom)))
        .thenCallRealMethod();
    when(geometryPixelProjector.toPixels(
            any(Geometry.class), anyInt(), anyInt(), anyInt(), anyInt()))
        .thenCallRealMethod();
    var fileMaskMock = mock(File.class);
    when(maskCreatorMock.apply(any())).thenReturn(fileMaskMock);

    var actual = subject.apply(tileMock, roofMultiPolygon);

    assertEquals(fileMaskMock, actual);
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(maskCreatorMock).apply(listCaptor.capture());
    var intersectedCoordinates = (List<BigDecimal>) listCaptor.getAllValues().getFirst();
    var expectedProjectedPolygon = expectedProjectedPolygonPixel();
    assertEquals(expectedProjectedPolygon, intersectedCoordinates);
  }

  private List<List<BigDecimal>> expectedProjectedPolygonPixel() {
    double[][] expectedPoints = {
      {322.37049746513367, 1024.0},
      {322.8131691813469, 1019.6071667671204},
      {321.5296914577484, 988.3340054750443},
      {316.5098766088486, 961.1309301257133},
      {311.32962560653687, 930.0187055468559},
      {302.41303580999374, 902.9765683412552},
      {285.22159188985825, 864.5288759469986},
      {267.66295498609543, 912.2433656454086},
      {200.61564230918884, 895.4337089657784},
      {213.95662033557892, 840.0618616342545},
      {135.21896237134933, 823.7350571155548},
      {141.56856924295425, 788.2308277487755},
      {31.335813760757446, 765.3733376264572},
      {41.10086315870285, 717.9806987643242},
      {96.1370347738266, 727.4548674821854},
      {102.80751556158066, 699.7689228057861},
      {121.16832637786865, 671.6001315116882},
      {143.5863561630249, 647.1795418262482},
      {170.38248020410538, 634.325458586216},
      {201.3962580561638, 629.1287338137627},
      {232.73091024160385, 631.7503155469894},
      {264.3864336013794, 642.1902037858963},
      {288.5692719221115, 660.7702872753143},
      {305.2794245481491, 687.4905619621277},
      {318.25323271751404, 718.2809292078018},
      {319.5367124080658, 749.5541261434555},
      {438.2047287225723, 787.7264637947083},
      {529.1139949560165, 815.297994852066},
      {951.3628605604172, 927.0795719027519},
      {1024.0, 942.3570665121078},
      {1024.0, 1024.0},
      {322.37049746513367, 1024.0}
    };
    List<List<BigDecimal>> result = new ArrayList<>();
    for (double[] point : expectedPoints) {
      List<BigDecimal> coordinate = new ArrayList<>();
      coordinate.add(BigDecimal.valueOf(point[0]));
      coordinate.add(BigDecimal.valueOf(point[1]));
      result.add(coordinate);
    }

    return result;
  }

  private MultiPolygon roofMultiPolygonForPoint() {
    // Polygon qui dépasse un peu du tile (en haut à droite)
    Coordinate[] coords =
        new Coordinate[] {
          new Coordinate(2.294, 48.858), // en dehors légèrement
          new Coordinate(2.295, 48.858),
          new Coordinate(2.295, 48.857),
          new Coordinate(2.294, 48.857),
          new Coordinate(2.294, 48.858)
        };
    LinearRing shell = geometryFactory.createLinearRing(coords);
    Polygon polygon = geometryFactory.createPolygon(shell);
    return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
  }

  private MultiPolygon multiPolygonFromTileForPoint() {
    // Polygon strictement dans une tuile autour de 2.293 - 2.294 / 48.856 - 48.857
    Coordinate[] coords =
        new Coordinate[] {
          new Coordinate(2.293, 48.857),
          new Coordinate(2.294, 48.857),
          new Coordinate(2.294, 48.856),
          new Coordinate(2.293, 48.856),
          new Coordinate(2.293, 48.857)
        };
    LinearRing shell = geometryFactory.createLinearRing(coords);
    Polygon polygon = geometryFactory.createPolygon(shell);
    return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
  }

  private MultiPolygon roofMultiPolygonForMultiPolygonIntersection() {
    var geometry =
        geometryConverterMock.readGeometryFromString(roofGeoJsonMultiPolygonStringValue());
    if (geometry instanceof MultiPolygon multiPolygon) {
      return multiPolygon;
    }
    return null;
  }

  @SneakyThrows
  private String roofGeoJsonMultiPolygonStringValue() {
    var fileContainingGeoJsonRoofDelimiter =
        new ClassPathResource("geometry/examples/roof-delimiter-multipolygon.json").getFile();
    return Files.readString(fileContainingGeoJsonRoofDelimiter.toPath());
  }
}
