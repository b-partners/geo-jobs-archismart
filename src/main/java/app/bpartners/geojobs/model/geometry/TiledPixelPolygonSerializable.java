package app.bpartners.geojobs.model.geometry;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import java.io.Serializable;
import java.util.List;

public record TiledPixelPolygonSerializable(
    Feature point, List<PolygonObjectTypeSerializable> polygons, int tileX, int tileY, int zoom)
    implements Serializable {}
