package app.bpartners.geojobs.model.geometry.route;

import app.bpartners.geojobs.model.geometry.quadrilateral.Alpha;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.OrientedQuadrilateral;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class AbstractRoute {
  private final Route route;
  private final AlphaConf alphaConf;
  private final Set<OrientedQuadrilateral> abstraction;

  public AbstractRoute(Route route, AlphaConf alphaConf) {
    this.route = route;
    this.alphaConf = alphaConf;
    this.abstraction = new Alpha(route, alphaConf).get();
  }
}
