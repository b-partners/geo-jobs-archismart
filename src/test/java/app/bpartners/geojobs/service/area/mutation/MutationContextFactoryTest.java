package app.bpartners.geojobs.service.area.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.DetectionMaskFromTileRetriever;
import app.bpartners.geojobs.service.area.mutation.model.AreaPictureHistoryResponse;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;

class MutationContextFactoryTest {
  private final DetectionMaskFromTileRetriever maskFromTileRetrieverMock = mock();
  private final MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  private final GeometryConverter geometryConverterMock = mock();
  private final GeodataApi geodataApiMock = mock();
  private final MutationContextFactory subject =
      new MutationContextFactory(
          maskFromTileRetrieverMock,
          machineDetectedTileRepositoryMock,
          geometryConverterMock,
          geodataApiMock);

  @Test
  void create_builds_mask_from_intersecting_tile_and_context_from_geodata_history() {
    var detection = Detection.builder().id("detection-1").zdjId("zdj-1").build();
    var tile =
        Tile.builder().id("tile-1").coordinates(new TileCoordinates().x(1).y(2).z(20)).build();
    when(machineDetectedTileRepositoryMock.findAllByZdjJobId("zdj-1"))
        .thenReturn(List.of(MachineDetectedTile.builder().id("m1").tile(tile).build()));

    var roofMultiPolygon = mock(MultiPolygon.class);
    var centroid = mock(Point.class);
    when(centroid.getX()).thenReturn(2.33);
    when(centroid.getY()).thenReturn(48.87);
    when(roofMultiPolygon.getCentroid()).thenReturn(centroid);
    var tileMultiPolygon = mock(MultiPolygon.class);
    when(tileMultiPolygon.intersects(roofMultiPolygon)).thenReturn(true);
    when(geometryConverterMock.getMultiPolygonFromTile(1, 2, 20)).thenReturn(tileMultiPolygon);

    var maskImageFile = new File("mask.png");
    when(maskFromTileRetrieverMock.apply(tile, roofMultiPolygon)).thenReturn(maskImageFile);

    var older =
        new AreaPictureHistoryResponse.DatedImage(
            2022, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/old.jpg"));
    var mostRecent =
        new AreaPictureHistoryResponse.DatedImage(
            2024, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/new.jpg"));
    when(geodataApiMock.getAreaPictureHistory(2.33, 48.87))
        .thenReturn(new AreaPictureHistoryResponse(List.of(mostRecent, older)));

    var actual = subject.create(detection, roofMultiPolygon);

    assertEquals(older, actual.older());
    assertEquals(mostRecent, actual.mostRecent());
    assertEquals(maskImageFile, actual.maskImageFile());
  }

  @Test
  void create_throws_when_geodata_returns_fewer_than_two_images() {
    var detection = Detection.builder().id("detection-1").zdjId("zdj-1").build();
    var tile =
        Tile.builder().id("tile-1").coordinates(new TileCoordinates().x(1).y(2).z(20)).build();
    when(machineDetectedTileRepositoryMock.findAllByZdjJobId("zdj-1"))
        .thenReturn(List.of(MachineDetectedTile.builder().id("m1").tile(tile).build()));

    var roofMultiPolygon = mock(MultiPolygon.class);
    var centroid = mock(Point.class);
    when(centroid.getX()).thenReturn(2.33);
    when(centroid.getY()).thenReturn(48.87);
    when(roofMultiPolygon.getCentroid()).thenReturn(centroid);
    var tileMultiPolygon = mock(MultiPolygon.class);
    when(tileMultiPolygon.intersects(roofMultiPolygon)).thenReturn(true);
    when(geometryConverterMock.getMultiPolygonFromTile(1, 2, 20)).thenReturn(tileMultiPolygon);
    when(maskFromTileRetrieverMock.apply(tile, roofMultiPolygon)).thenReturn(new File("mask.png"));

    var onlyImage =
        new AreaPictureHistoryResponse.DatedImage(
            2024, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/new.jpg"));
    when(geodataApiMock.getAreaPictureHistory(2.33, 48.87))
        .thenReturn(new AreaPictureHistoryResponse(List.of(onlyImage)));

    assertThrows(IllegalStateException.class, () -> subject.create(detection, roofMultiPolygon));
  }

  @Test
  void findTileIntersecting_returns_first_tile_whose_multi_polygon_intersects_the_roof() {
    var detection = Detection.builder().id("detection-1").zdjId("zdj-1").build();
    var tile1 =
        Tile.builder().id("tile-1").coordinates(new TileCoordinates().x(1).y(2).z(3)).build();
    var tile2 =
        Tile.builder().id("tile-2").coordinates(new TileCoordinates().x(4).y(5).z(6)).build();
    when(machineDetectedTileRepositoryMock.findAllByZdjJobId("zdj-1"))
        .thenReturn(
            List.of(
                MachineDetectedTile.builder().id("m1").tile(tile1).build(),
                MachineDetectedTile.builder().id("m2").tile(tile2).build()));
    var roofMultiPolygon = mock(MultiPolygon.class);
    var nonIntersectingMultiPolygon = mock(MultiPolygon.class);
    when(nonIntersectingMultiPolygon.intersects(roofMultiPolygon)).thenReturn(false);
    var intersectingMultiPolygon = mock(MultiPolygon.class);
    when(intersectingMultiPolygon.intersects(roofMultiPolygon)).thenReturn(true);
    when(geometryConverterMock.getMultiPolygonFromTile(1, 2, 3))
        .thenReturn(nonIntersectingMultiPolygon);
    when(geometryConverterMock.getMultiPolygonFromTile(4, 5, 6))
        .thenReturn(intersectingMultiPolygon);

    var actual =
        invokePrivate(
            "findTileIntersecting",
            new Class<?>[] {Detection.class, MultiPolygon.class},
            detection,
            roofMultiPolygon);

    assertEquals(tile2, actual);
  }

  @Test
  void findTileIntersecting_throws_when_no_tile_intersects_the_roof() {
    var detection = Detection.builder().id("detection-1").zdjId("zdj-1").build();
    var tile1 =
        Tile.builder().id("tile-1").coordinates(new TileCoordinates().x(1).y(2).z(3)).build();
    when(machineDetectedTileRepositoryMock.findAllByZdjJobId("zdj-1"))
        .thenReturn(List.of(MachineDetectedTile.builder().id("m1").tile(tile1).build()));
    var roofMultiPolygon = mock(MultiPolygon.class);
    var nonIntersectingMultiPolygon = mock(MultiPolygon.class);
    when(nonIntersectingMultiPolygon.intersects(roofMultiPolygon)).thenReturn(false);
    when(geometryConverterMock.getMultiPolygonFromTile(1, 2, 3))
        .thenReturn(nonIntersectingMultiPolygon);

    var cause =
        assertThrows(
            InvocationTargetException.class,
            () ->
                invokePrivate(
                    "findTileIntersecting",
                    new Class<?>[] {Detection.class, MultiPolygon.class},
                    detection,
                    roofMultiPolygon));

    assertInstanceOf(IllegalStateException.class, cause.getCause());
  }

  @Test
  void tileMultiPolygon_delegates_to_geometry_converter() {
    var tile = Tile.builder().coordinates(new TileCoordinates().x(7).y(8).z(9)).build();
    var expected = mock(MultiPolygon.class);
    when(geometryConverterMock.getMultiPolygonFromTile(7, 8, 9)).thenReturn(expected);

    var actual = invokePrivate("tileMultiPolygon", new Class<?>[] {Tile.class}, tile);

    assertSame(expected, actual);
  }

  @Test
  void asMultiPolygon_returns_same_instance_when_already_a_multi_polygon() {
    var multiPolygon = mock(MultiPolygon.class);

    var actual =
        invokeStaticPrivate("asMultiPolygon", new Class<?>[] {Geometry.class}, multiPolygon);

    assertSame(multiPolygon, actual);
  }

  @Test
  void asMultiPolygon_wraps_a_polygon_into_a_multi_polygon() {
    var geometryFactory = new GeometryFactory();
    var polygon =
        geometryFactory.createPolygon(
            new Coordinate[] {
              new Coordinate(0, 0), new Coordinate(1, 0), new Coordinate(1, 1), new Coordinate(0, 0)
            });

    var actual = invokeStaticPrivate("asMultiPolygon", new Class<?>[] {Geometry.class}, polygon);

    var actualMultiPolygon = assertInstanceOf(MultiPolygon.class, actual);
    assertEquals(1, actualMultiPolygon.getNumGeometries());
    assertSame(polygon, actualMultiPolygon.getGeometryN(0));
  }

  @Test
  void asMultiPolygon_throws_for_unsupported_geometry_type() {
    var geometryFactory = new GeometryFactory();
    Point point = geometryFactory.createPoint(new Coordinate(0, 0));

    var cause =
        assertThrows(
            InvocationTargetException.class,
            () -> invokeStaticPrivate("asMultiPolygon", new Class<?>[] {Geometry.class}, point));

    assertInstanceOf(IllegalArgumentException.class, cause.getCause());
  }

  @SneakyThrows
  private Object invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) {
    Method method = MutationContextFactory.class.getDeclaredMethod(methodName, paramTypes);
    method.setAccessible(true);
    return method.invoke(subject, args);
  }

  @SneakyThrows
  private Object invokeStaticPrivate(String methodName, Class<?>[] paramTypes, Object... args) {
    Method method = MutationContextFactory.class.getDeclaredMethod(methodName, paramTypes);
    method.setAccessible(true);
    return method.invoke(null, args);
  }
}
