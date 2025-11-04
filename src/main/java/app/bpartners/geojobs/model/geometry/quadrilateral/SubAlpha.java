package app.bpartners.geojobs.model.geometry.quadrilateral;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.geometry.polygon.EnvelopeAsPolygon;
import app.bpartners.geojobs.model.geometry.polygon.LongestInteriorLine;
import app.bpartners.geojobs.model.geometry.polygon.MaximalPolygonFromEdges;
import app.bpartners.geojobs.model.geometry.polygon.OrientedBoundingBox;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.Quadrilateral;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

@Accessors(fluent = true)
@Getter
public class SubAlpha implements Supplier<Quadrilateral> {

  private final MultiPolygon p;
  private final Polygon obb_inter_p;
  private final Quadrilateral quadrilateral;

  public SubAlpha(Polygon polygon) {
    this.p = geometryFactory.createMultiPolygon(new Polygon[] {polygon});
    this.obb_inter_p = obb_inter_p(polygon);
    this.quadrilateral = quadrilateralFromObbInterP(obb_inter_p);
  }

  public SubAlpha(MultiPolygon polygons) {
    this.p = polygons;
    this.obb_inter_p = obb_inter_p(polygons);
    this.quadrilateral = quadrilateralFromObbInterP(obb_inter_p);
  }

  @Override
  public Quadrilateral get() {
    return quadrilateral;
  }

  private static Polygon obb_inter_p(MultiPolygon polygons) {
    Set<Polygon> subAlpha_per_polygon = new HashSet<>();
    for (int n = 0; n < polygons.getNumGeometries(); n++) {
      var nthGeometry = polygons.getGeometryN(n);
      subAlpha_per_polygon.add(new SubAlpha((Polygon) nthGeometry).obb_inter_p());
    }

    return subAlpha_per_polygon.stream().findFirst().get();
  }

  private static Polygon obb_inter_p(Polygon p) {
    var envelopeOfLil = new EnvelopeAsPolygon(new LongestInteriorLine(p).get()).get();
    var envelopeOfLil_inter_p = (Polygon) envelopeOfLil.intersection(p);
    var obb = new OrientedBoundingBox(envelopeOfLil_inter_p).get();
    return (Polygon) obb.intersection(p);
  }

  private static Quadrilateral quadrilateralFromObbInterP(Polygon obb_inter_p) {
    return new Quadrilateral(new MaximalPolygonFromEdges(obb_inter_p, 4).get());
  }
}
