package app.bpartners.geojobs.model.geometry.lidar.planes.postprocessing;

import static app.bpartners.geojobs.service.lidar.model.LidarClass.BATIMENT;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.Plane3DDelimitationFixer;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Plane3DDelimitationFixerTest {
  @Test
  void basic_when_plane_should_change() {
    var points =
        Set.of(
            // Cell (0,0)
            new LasPointGeometry(0.1, 0.1, 0, BATIMENT),
            new LasPointGeometry(0.3, 0.2, 0, BATIMENT),
            new LasPointGeometry(0.7, 0.5, 0, BATIMENT),

            // Cell (0,1)
            new LasPointGeometry(0.1, 1.1, 0, BATIMENT),
            new LasPointGeometry(0.5, 1.3, 0, BATIMENT),
            new LasPointGeometry(0.8, 1.2, 0, BATIMENT),

            // Cell (1,0)
            new LasPointGeometry(1.1, 0.2, 0, BATIMENT),
            new LasPointGeometry(1.3, 0.4, 0, BATIMENT),
            new LasPointGeometry(1.5, 0.7, 0, BATIMENT),

            // Cell (1,1)
            new LasPointGeometry(1.2, 1.1, 0, BATIMENT),
            new LasPointGeometry(1.4, 1.3, 0, BATIMENT),
            new LasPointGeometry(1.8, 1.5, 0, BATIMENT));

    var plane = Plane3D.builder().a(0).b(0).c(1).d(0).points(points).build();

    var fixer = new Plane3DDelimitationFixer(1, 0, 1, 0.1);

    var result = fixer.apply(plane, points);

    assertNotSame(plane, result);
  }

  @Test
  void basic_when_plane_should_remains_the_same() {
    var points =
        Set.of(
            new LasPointGeometry(0, 0, 0, BATIMENT),
            new LasPointGeometry(0, 3, 0, BATIMENT),
            new LasPointGeometry(6, 0, 0, BATIMENT));
    var plane = Plane3D.builder().a(0).b(0).c(1).d(0).points(points).build();

    var fixer = new Plane3DDelimitationFixer(0, 0, 1, 0.1);

    var result = fixer.apply(plane, points);

    assertSame(plane, result);
  }
}
