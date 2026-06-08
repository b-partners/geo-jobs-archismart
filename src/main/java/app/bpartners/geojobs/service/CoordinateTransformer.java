package app.bpartners.geojobs.service;

import java.util.function.Function;
import lombok.SneakyThrows;
import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;

@Component
public class CoordinateTransformer implements Function<Geometry, Geometry> {

  // WGS84  → EPSG:4326
  // Lambert-93 → EPSG:2154

  @SneakyThrows
  @Override
  public Geometry apply(Geometry geomWGS84) throws MismatchedDimensionException {

    CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326", true); // true = lon/lat order
    CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:2154");

    MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);

    return JTS.transform(geomWGS84, transform);
  }

  @SneakyThrows
  public Geometry convertToSwissCoordinates(Geometry geomWGS84)
      throws MismatchedDimensionException {

    CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326", true);
    CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:2056");

    MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);

    return JTS.transform(geomWGS84, transform);
  }
}
