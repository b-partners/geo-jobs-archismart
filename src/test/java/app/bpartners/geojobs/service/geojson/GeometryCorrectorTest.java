package app.bpartners.geojobs.service.geojson;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

class GeometryCorrectorTest {
  private final GeometryFactory geometryFactory = new GeometryFactory();
  private final GeometryCorrector subject = new GeometryCorrector();

  @Test
  void does_not_throw_on_self_intersecting_input() {
    // "bowtie" ring: self-intersecting -> invalid geometry. Before the fix this made
    // GeometryPrecisionReducer.reduce throw "Reduction failed, possible invalid input".
    var bowtie = bowtieMultiPolygon();
    assertFalse(bowtie.isValid(), "sanity: input must be invalid to guard the regression");

    var corrected = assertDoesNotThrow(() -> subject.apply(bowtie));

    assertTrue(corrected.isValid(), "corrected geometry must be valid");
  }

  @Test
  void keeps_a_valid_input_valid() {
    var square = geometryFactory.createMultiPolygon(new Polygon[] {squarePolygon()});
    assertTrue(square.isValid());

    var corrected = subject.apply(square);

    assertTrue(corrected.isValid());
    assertFalse(corrected.isEmpty());
  }

  private MultiPolygon bowtieMultiPolygon() {
    var ring =
        geometryFactory.createLinearRing(
            new Coordinate[] {
              new Coordinate(2.9817, 50.4458),
              new Coordinate(2.9819, 50.4460),
              new Coordinate(2.9819, 50.4458),
              new Coordinate(2.9817, 50.4460),
              new Coordinate(2.9817, 50.4458)
            });
    var polygon = geometryFactory.createPolygon(ring);
    return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
  }

  private Polygon squarePolygon() {
    LinearRing ring =
        geometryFactory.createLinearRing(
            new Coordinate[] {
              new Coordinate(2.9817, 50.4458),
              new Coordinate(2.9819, 50.4458),
              new Coordinate(2.9819, 50.4460),
              new Coordinate(2.9817, 50.4460),
              new Coordinate(2.9817, 50.4458)
            });
    return geometryFactory.createPolygon(ring);
  }
}
