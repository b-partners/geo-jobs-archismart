package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.geometry.RoofDetails;
import java.math.BigDecimal;
import java.util.List;
import org.locationtech.jts.geom.MultiPolygon;

public interface BuildingFinder {
  MultiPolygon getBuildingMultiPolygon(Point point);

  MultiPolygon getBuildingMultiPolygon(List<BigDecimal> coordinates);

  MultiPolygon getBuildingMultiPolygon(String address);

  List<RoofDetails> retrieveRoofPolygonsFrom(List<List<BigDecimal>> lonLatPolygonCoordinates);
}
