package app.bpartners.geojobs.service.gouv.fr.rnb.component.geometry;

import java.math.BigDecimal;
import java.util.List;

public sealed interface GeometryCoordinates
    permits PointCoordinates, PolygonCoordinates, MultiPolygonCoordinates {}

record PointCoordinates(List<BigDecimal> coordinates) implements GeometryCoordinates {}

record PolygonCoordinates(List<List<List<BigDecimal>>> coordinates)
    implements GeometryCoordinates {}

record MultiPolygonCoordinates(List<List<List<List<BigDecimal>>>> coordinates)
    implements GeometryCoordinates {}
