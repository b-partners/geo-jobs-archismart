package app.bpartners.geojobs.model.geometry.feature;

import static lombok.AccessLevel.PRIVATE;

import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.TileCoordinatesFromFileName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.locationtech.jts.geom.Polygon;

@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = PRIVATE)
@Builder(toBuilder = true)
public class Feature {
  private final String filename;
  private final String label;
  private final double confidence;
  private final Polygon geometry;

  private final IntXY imgSize;
  private final int zoom;
  private final IntXY tileCoordinate;

  public Feature(
      String filename,
      String label,
      double confidence,
      Polygon geometry,
      IntXY imgSize,
      boolean is_z_x_y_dot_filetype) {
    this.filename = filename;
    this.label = label;
    this.confidence = confidence;
    this.geometry = geometry;
    this.imgSize = imgSize;
    var coordExtractor = new TileCoordinatesFromFileName(is_z_x_y_dot_filetype);
    this.zoom = coordExtractor.z(filename);
    this.tileCoordinate = new IntXY(coordExtractor.x(filename), coordExtractor.y(filename));
  }
}
