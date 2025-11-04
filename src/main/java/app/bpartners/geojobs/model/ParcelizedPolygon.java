package app.bpartners.geojobs.model;

import static java.lang.Math.pow;

import app.bpartners.geojobs.model.geometry.Envelope;
import app.bpartners.geojobs.model.geometry.area.IsAreaOfParcel;
import app.bpartners.geojobs.model.geometry.area.SquareDegree;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

public class ParcelizedPolygon {
  @Getter private final Polygon polygon;
  @Getter private final ArcgisRasterZoom arcgisRasterZoom;
  @Getter private final Set<Polygon> parcels;

  private final IsAreaOfParcel isAreaOfParcel;

  public ParcelizedPolygon(Polygon polygon, ArcgisRasterZoom arcgisRasterZoom) {
    this(polygon, arcgisRasterZoom, new ArcgisRasterZoom(20), new SquareDegree(2 * pow(10, -4)));
  }

  public ParcelizedPolygon(
      Polygon polygon,
      ArcgisRasterZoom targetZoom,
      ArcgisRasterZoom referenceZoom,
      SquareDegree maxParcelAreaAtReferenceZoom) {
    this.polygon = polygon;
    this.arcgisRasterZoom = targetZoom;
    this.isAreaOfParcel = new IsAreaOfParcel(referenceZoom, maxParcelAreaAtReferenceZoom);
    this.parcels = parcelize(polygon);
  }

  private Set<Polygon> parcelize(Polygon thatPolygon) {
    var intersection = thatPolygon.intersection(this.polygon);
    if (isAreaOfParcel.test(intersection, arcgisRasterZoom)) {
      return Set.of((Polygon) intersection);
    }

    var envelope = thatPolygon.getEnvelopeInternal();
    var minX = envelope.getMinX();
    var maxX = envelope.getMaxX();
    var minY = envelope.getMinY();
    var maxY = envelope.getMaxY();
    var centroidX = (minX + maxX) / 2.0;
    var centroidY = (minY + maxY) / 2.0;

    var upperLeftEnvelope = new Envelope(minX, centroidX, centroidY, maxY);
    var upperRightEnvelope = new Envelope(centroidX, maxX, centroidY, maxY);
    var lowerLeftEnvelope = new Envelope(minX, centroidX, minY, centroidY);
    var lowerRightEnvelope = new Envelope(centroidX, maxX, minY, centroidY);
    var upperLeft = parcelize(quadrilateralFromEnvelope(upperLeftEnvelope));
    var upperRight = parcelize(quadrilateralFromEnvelope(upperRightEnvelope));
    var lowerLeft = parcelize(quadrilateralFromEnvelope(lowerLeftEnvelope));
    var lowerRight = parcelize(quadrilateralFromEnvelope(lowerRightEnvelope));

    return Stream.concat(
            Stream.concat(upperLeft.stream(), upperRight.stream()),
            Stream.concat(lowerLeft.stream(), lowerRight.stream()))
        .collect(Collectors.toSet());
  }

  private Polygon quadrilateralFromEnvelope(Envelope envelope) {
    var geometryFactory = new GeometryFactory();
    var poly =
        geometryFactory.createLinearRing(
            new Coordinate[] {
              new Coordinate(envelope.minX(), envelope.minY()),
              new Coordinate(envelope.maxX(), envelope.minY()),
              new Coordinate(envelope.maxX(), envelope.maxY()),
              new Coordinate(envelope.minX(), envelope.maxY()),
              new Coordinate(envelope.minX(), envelope.minY())
            });
    return geometryFactory.createPolygon(poly);
  }
}
