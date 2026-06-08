package app.bpartners.geojobs.service.cityjson.texture.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Coordinate;

public record CityJsonWithVertices(
    ObjectNode json, List<Coordinate> vertices, CoordinateReferenceSystem crs) {}
