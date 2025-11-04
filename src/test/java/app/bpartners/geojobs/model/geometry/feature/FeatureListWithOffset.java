package app.bpartners.geojobs.model.geometry.feature;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.VGG;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
public class FeatureListWithOffset implements Supplier<List<Feature>> {

  private final List<Feature> features;

  public FeatureListWithOffset(
      VGG vgg, IntXY imageResolution, boolean is_z_x_y_dot_filetype, IntXY origin) {
    var featuresWithoutTileOffset =
        new FeatureListWithoutOffset(vgg, imageResolution, is_z_x_y_dot_filetype).get();
    this.features = withXYOffset(featuresWithoutTileOffset, origin);
  }

  @Override
  public List<Feature> get() {
    return features;
  }

  private List<Feature> withXYOffset(List<Feature> features, IntXY origin) {
    return features.stream().map(feature -> withXYOffset(feature, origin)).toList();
  }

  private Feature withXYOffset(Feature f, IntXY origin) {
    return f.toBuilder()
        .geometry(withXYOffset(f.geometry(), f.tileCoordinate(), origin, f.imgSize()))
        .build();
  }

  private Polygon withXYOffset(Polygon p, IntXY tileXY, IntXY origin, IntXY imgSize) {
    var oldCoordinates = p.getCoordinates();
    var newCoordinates = new Coordinate[oldCoordinates.length];
    for (int i = 0; i < newCoordinates.length; i++) {
      var oldCoordinateI = oldCoordinates[i];
      newCoordinates[i] = applyOffset(oldCoordinateI, tileXY, origin, imgSize);
    }

    return geometryFactory.createPolygon(newCoordinates);
  }

  private Coordinate applyOffset(Coordinate c, IntXY tileXY, IntXY origin, IntXY imgSize) {
    var offset = offset(tileXY, origin, imgSize);
    return new Coordinate(c.x + offset.x(), c.y + offset.y());
  }

  public static Coordinate unapplyOffset(Coordinate c, IntXY tileXY, IntXY origin, IntXY imgSize) {
    var offset = offset(tileXY, origin, imgSize);
    return new Coordinate(c.x - offset.x(), c.y - offset.y());
  }

  private static IntXY offset(IntXY tileXY, IntXY origin, IntXY imgSize) {
    int xOffset = (tileXY.x() - origin.x()) * imgSize.x();
    int yOffset = (tileXY.y() - origin.y()) * imgSize.y();
    return new IntXY(xOffset, yOffset);
  }
}
