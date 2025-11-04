package app.bpartners.geojobs.model.geometry.polygon;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import java.util.function.BiFunction;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public class MultiPolygonUnion implements BiFunction<MultiPolygon, Polygon, MultiPolygon> {
  @Override
  public MultiPolygon apply(MultiPolygon polygons, Polygon polygon) {
    Geometry newUnion_asGeometry = polygons.union(polygon);
    polygons =
        newUnion_asGeometry instanceof MultiPolygon
            ? (MultiPolygon) newUnion_asGeometry
            : geometryFactory.createMultiPolygon(new Polygon[] {(Polygon) newUnion_asGeometry});
    return polygons;
  }
}
