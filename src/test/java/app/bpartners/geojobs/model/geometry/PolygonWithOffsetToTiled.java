package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.geometry.route.ObjectType.routeTypeFrom;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.feature.FeatureListWithOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@AllArgsConstructor
public class PolygonWithOffsetToTiled implements Function<Polygon, TiledPolygon> {

  private final IntXY origin;
  private final TilingConf tilingConf;
  private final boolean is_z_x_y_dot_filetype;

  @Override
  public TiledPolygon apply(Polygon p) {
    Map<String, String> userData = (Map) p.getUserData();
    var label = userData.get("label");
    var tileXY = tileXY(userData.get("filename"));
    return new TiledPolygon(unapplyOffset(p, tileXY), routeTypeFrom(label), origin, tilingConf);
  }

  private Polygon unapplyOffset(Polygon p, IntXY tileXY) {
    var imgSize = tilingConf.imgSize();
    var imgSizeXY = new IntXY(imgSize, imgSize);
    return geometryFactory.createPolygon(
        Arrays.stream(p.getCoordinates())
            .map(c -> FeatureListWithOffset.unapplyOffset(c, tileXY, origin, imgSizeXY))
            .toArray(Coordinate[]::new));
  }

  private IntXY tileXY(String filename) {
    var coordExtractor = new TileCoordinatesFromFileName(is_z_x_y_dot_filetype);
    return new IntXY(coordExtractor.x(filename), coordExtractor.y(filename));
  }
}
