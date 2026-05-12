package app.bpartners.geojobs.model.lidar.planes.topology.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.polygonize.Polygonizer;

public class PolygonSplitter {
  private PolygonSplitter() {}

  @SuppressWarnings("all")
  public static List<Polygon> split(Polygon polygon, LineString splitter) {
    var nodedLinework = polygon.getBoundary().union(splitter);
    var polygonizer = new Polygonizer();
    polygonizer.add(nodedLinework);

    var polygons = polygonizer.getPolygons();
    if (polygons.isEmpty()) {
      return List.of(polygon);
    }
    return new ArrayList<Polygon>((Collection<Polygon>) polygons);
  }
}
