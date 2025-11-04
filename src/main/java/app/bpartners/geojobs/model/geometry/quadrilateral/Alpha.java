package app.bpartners.geojobs.model.geometry.quadrilateral;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.geometry.GeometryDiff;
import app.bpartners.geojobs.model.geometry.polygon.MultiPolygonUnion;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.ContinuationOrientation;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.OrientedQuadrilateral;
import app.bpartners.geojobs.model.geometry.route.Route;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

@Accessors(fluent = true)
@Getter
@Slf4j
public class Alpha implements Supplier<Set<OrientedQuadrilateral>> {

  private final MultiPolygon p;
  private final ContinuationOrientation continuationOrientation;
  private final AlphaConf conf;
  private final Set<OrientedQuadrilateral> oqSet;

  public Alpha(Route route, AlphaConf conf) {
    var multipolygon = geometryFactory.createMultiPolygon(new Polygon[] {route.polygon()});
    this.p = multipolygon;
    this.continuationOrientation = route.type().continuationOrientation();
    this.conf = conf;
    oqSet = oqSet(multipolygon, continuationOrientation, conf);
  }

  private static Set<OrientedQuadrilateral> oqSet(
      MultiPolygon p, ContinuationOrientation continuationOrientation, AlphaConf conf) {
    try {
      return fallibleAlpha(p, continuationOrientation, conf);
    } catch (Exception e) {
      log.error(String.format("Alpha failed: polygon=%s", p), e);
      return Set.of();
    }
  }

  @Override
  public Set<OrientedQuadrilateral> get() {
    return oqSet;
  }

  private static Set<OrientedQuadrilateral> fallibleAlpha(
      MultiPolygon p, ContinuationOrientation continuationOrientation, AlphaConf conf) {
    Set<OrientedQuadrilateral> oqSet = new HashSet<>();

    var pMinus = p;
    var unionOf_obbInterP = geometryFactory.createMultiPolygon();
    do {
      var subAlpha = new SubAlpha(pMinus);
      oqSet.add(new OrientedQuadrilateral(subAlpha.get(), continuationOrientation));
      unionOf_obbInterP = new MultiPolygonUnion().apply(unionOf_obbInterP, subAlpha.obb_inter_p());
      pMinus = new GeometryDiff(conf.minAbstractArea()).apply(p, unionOf_obbInterP);
    } while (unionOf_obbInterP.getArea() / p.getArea() < conf.minCoverageOfAbstractedArea());

    return oqSet;
  }
}
