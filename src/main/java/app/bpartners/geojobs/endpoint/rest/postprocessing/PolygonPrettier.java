package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.Route;
import java.util.Set;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

@Slf4j
public class PolygonPrettier implements Function<Set<Polygon>, Set<Polygon>> {
  private final PrettyConf prettyConf;

  public PolygonPrettier(PrettyConf prettyConf) {
    this.prettyConf = prettyConf;
  }

  private Polygon simplify(Polygon p) {
    var buffer = prettyConf.dpbThreshold() / 2;
    var distanceThreshold = prettyConf.dpbThreshold();
    try {
      var prettyP =
          (Polygon) DouglasPeuckerSimplifier.simplify(p.buffer(buffer), distanceThreshold);
      prettyP.setUserData(p.getUserData());
      return prettyP;
    } catch (Exception e) {
      log.error(String.format("Error simplifying polygon=%s", p), e);
      return p;
    }
  }

  @Override
  public Set<Polygon> apply(Set<Polygon> unified) {
    return unified.stream().map(this::simplify).collect(toSet());
  }

  public Set<Route> pretty(Set<Route> unified) {
    return unified.stream()
        .map(tP -> new Route(simplify(tP.polygon()), tP.type()))
        .collect(toSet());
  }
}
