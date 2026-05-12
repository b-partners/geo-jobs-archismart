package app.bpartners.geojobs.service.cityjson.texture.factory;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;

import app.bpartners.geojobs.repository.model.cityjson.CityJSONTexture;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.cityjson.texture.model.RasterInfo;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

public class RasterInfoFactory {
  private RasterInfoFactory() {}

  public static RasterInfo create(
      GeometrySquareMeterArea geometrySquareMeter, CityJSONTexture texture) {

    Point originGeometry =
        geometryFactory.createPoint(
            new Coordinate(texture.getTopLeftLongitude(), texture.getTopLeftLatitude()));

    Geometry projected = geometrySquareMeter.project(originGeometry, WGS84, LAMBERT_93);

    Coordinate o = projected.getCoordinate();

    return new RasterInfo(
        o.getX(),
        o.getY(),
        texture.getPixelWidth(),
        texture.getPixelHeight(),
        texture.getShearX(),
        texture.getShearY(),
        texture.getImageWidth(),
        texture.getImageHeight(),
        LAMBERT_93);
  }
}
