package app.bpartners.geojobs.service.lidar.model.geometry;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.lidar.planes.PlaneDelimitation;
import app.bpartners.geojobs.model.lidar.planes.PlaneDelimitation.PlaneDelimitationConf;
import app.bpartners.geojobs.model.lidar.planes.conf.RangedConf;
import app.bpartners.geojobs.model.lidar.planes.conf.RangedConf.IntegerRangedConf;
import app.bpartners.geojobs.utils.lidar.LidarRoofsAnalysisProcessorCreator;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

class PlaneDelimitationTest {
  private static final LidarRoofsAnalysisProcessorCreator processorCreator =
      new LidarRoofsAnalysisProcessorCreator();
  private static final double CONCAVE_RATIO = 0.2;
  private static final double DPS_SIMPLIFICATION = 0.6;
  private static final double LR_SIMPLIFICATION = 0;
  private static final PlaneDelimitationConf CONF =
      new PlaneDelimitationConf(
          RangedConf.from(
              new IntegerRangedConf<>(Integer.MIN_VALUE, Integer.MAX_VALUE, CONCAVE_RATIO)),
          DPS_SIMPLIFICATION,
          LR_SIMPLIFICATION);

  @Test
  void guess_delimitation_ok() {
    var roofGeometry1 = roofGeometry1();
    var roofGeometries = Set.of(roofGeometry1);
    var processor = processorCreator.create(roofGeometries);
    var processResult = processor.from(roofGeometries);

    var points = processResult.getData(roofGeometry1).roof().points();
    var actual = new PlaneDelimitation(CONF, points);

    assertTrue(actual.getPolygon().isValid());
  }

  private static Geometry roofGeometry1() {
    var roof1Coordinates =
        new Coordinate[] {
          new Coordinate(2.243891733457616, 48.82448842864014),
          new Coordinate(2.243947393505863, 48.82437718542337),
          new Coordinate(2.244038835011281, 48.82440597780899),
          new Coordinate(2.2440209442821413, 48.82445309258651),
          new Coordinate(2.244197863717403, 48.8244975898354),
          new Coordinate(2.24422768160008, 48.82447010624497),
          new Coordinate(2.24432906240051, 48.824487119898066),
          new Coordinate(2.244263463059525, 48.82456695311532),
          new Coordinate(2.243891733457616, 48.82448842864014)
        };
    return geometryFactory.createPolygon(roof1Coordinates);
  }
}
