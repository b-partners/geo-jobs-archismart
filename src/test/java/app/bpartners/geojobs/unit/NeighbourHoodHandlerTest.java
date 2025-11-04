package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.postprocessing.NeighbourHoodHandler;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.route.ObjectType;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

public class NeighbourHoodHandlerTest {
  NeighbourHoodHandler subject = new NeighbourHoodHandler(3);

  @Test
  void round_3() {
    var actual = subject.aroundN(polygons());

    assertEquals(3, actual.size());
  }

  Set<TiledPolygon> polygons() {
    TilingConf conf = new TilingConf(1, 256);
    ObjectType type = ObjectType.road;
    Polygon dummyPolygon = null;

    return new HashSet<>(
        Set.of(
            new TiledPolygon(dummyPolygon, type, new IntXY(4, 4), conf),
            new TiledPolygon(dummyPolygon, type, new IntXY(5, 4), conf),
            new TiledPolygon(dummyPolygon, type, new IntXY(6, 6), conf),
            new TiledPolygon(dummyPolygon, type, new IntXY(5, 5), conf),
            new TiledPolygon(dummyPolygon, type, new IntXY(7, 7), conf),
            new TiledPolygon(dummyPolygon, type, new IntXY(2, 5), conf)));
  }
}
