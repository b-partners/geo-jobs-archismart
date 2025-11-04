package app.bpartners.geojobs.model.parcelization;

import static java.lang.Math.pow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.ArcgisRasterZoom;
import app.bpartners.geojobs.model.ParcelizedPolygon;
import app.bpartners.geojobs.model.geometry.area.SquareDegree;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

class ParcelizedPolygonTest {

  public Polygon ivandryPolygon() {
    var start = new Coordinate(47.530304168682925, -18.863332120399996);
    return new GeometryFactory()
        .createPolygon(
            new Coordinate[] {
              start,
              new Coordinate(47.527370898166588, -18.876071533552416),
              new Coordinate(47.546850822877644, -18.876213867894698),
              new Coordinate(47.548505488297103, -18.863332120399996),
              start // note(LinearRing)
            });
  }

  @Test
  void nb_parcels_is_linear_x4_to_target_zoom_increment() {
    var ivandryPolygon = ivandryPolygon();

    assertEquals(
        1, new ParcelizedPolygon(ivandryPolygon, new ArcgisRasterZoom(19)).getParcels().size());
    assertEquals(
        4, new ParcelizedPolygon(ivandryPolygon, new ArcgisRasterZoom(20)).getParcels().size());
    assertEquals(
        16, new ParcelizedPolygon(ivandryPolygon, new ArcgisRasterZoom(21)).getParcels().size());
  }

  @Test
  void parcelize_ivandry_is_not_parcelized_with_big_enough_custom_parcel_area() {
    var ivandryPolygon = ivandryPolygon();

    var parcelized_ivandry =
        new ParcelizedPolygon(
            ivandryPolygon,
            new ArcgisRasterZoom(20),
            new ArcgisRasterZoom(20),
            new SquareDegree(3 * pow(10, -4)));

    assertEquals(1, parcelized_ivandry.getParcels().size());
  }

  @Test
  void ivandry_is_parcelized_with_tiny_custom_parcel_area() {
    var ivandryPolygon = ivandryPolygon();

    var parcelized_ivandry =
        new ParcelizedPolygon(
            ivandryPolygon,
            new ArcgisRasterZoom(20),
            new ArcgisRasterZoom(20),
            new SquareDegree(2 * pow(10, -5)));

    assertEquals(16, parcelized_ivandry.getParcels().size());
  }

  @Test
  void ivandry_is_parcelized_when_target_zoom_and_parcel_area_differ() {
    var ivandryPolygon = ivandryPolygon();

    var parcelized_ivandry =
        new ParcelizedPolygon(
            ivandryPolygon,
            new ArcgisRasterZoom(19),
            new ArcgisRasterZoom(20),
            new SquareDegree(2 * pow(10, -5)));

    assertEquals(4, parcelized_ivandry.getParcels().size());
  }
}
