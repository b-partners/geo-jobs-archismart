package app.bpartners.geojobs.model.geometry.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.lidar.Polygon3DArea;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.algorithm.OBB3DComputer;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.ChimneyFixer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

class ChimneyFixerTest {
  private static final OBB3DComputer obb3DComputerMock = mock(OBB3DComputer.class);
  private static final ChimneyFixer subject = new ChimneyFixer(2, obb3DComputerMock, null);

  @Test
  void should_update_all_chimney() {
    var roof = roof();
    var planes = List.of(roof, chimney());

    when(obb3DComputerMock.apply(any())).thenReturn(polygon());

    var actual = subject.apply(planes);

    var actualRoof = actual.getFirst();
    var actualChimney = actual.get(1);

    assertEquals(actualRoof.getDelimitation(), roof.getDelimitation());
    assertEquals(actualChimney.getDelimitation(), polygon());
  }

  private static Plane3D roof() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(0, 0, 0),
          new Coordinate(10, 0, 0),
          new Coordinate(10, 10, 0),
          new Coordinate(0, 10, 0),
          new Coordinate(0, 0, 0)
        };

    var area = mock(Polygon3DArea.class);
    when(area.getValue()).thenReturn(100.d);

    var delimitation = geometryFactory.createPolygon(coordinates);
    return Plane3D.builder().delimitation(delimitation).area(area).build();
  }

  private static Plane3D chimney() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(4, 4, 2),
          new Coordinate(6, 4, 2),
          new Coordinate(6, 6, 2),
          new Coordinate(4, 6, 2),
          new Coordinate(4, 4, 2)
        };
    var delimitation = geometryFactory.createPolygon(coordinates);

    var area = mock(Polygon3DArea.class);
    when(area.getValue()).thenReturn(1.5d);

    return Plane3D.builder()
        .delimitation(delimitation)
        .convexDelimitation(delimitation)
        .area(area)
        .build();
  }

  private static Polygon polygon() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(4, 4, 3),
          new Coordinate(6, 4, 3),
          new Coordinate(6, 6, 3),
          new Coordinate(4, 6, 3),
          new Coordinate(4, 4, 3)
        };
    return geometryFactory.createPolygon(coordinates);
  }
}
