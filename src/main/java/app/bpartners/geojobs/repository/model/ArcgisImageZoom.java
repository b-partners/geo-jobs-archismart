package app.bpartners.geojobs.repository.model;

import lombok.Getter;

/**
 * Summary: ArcGisVectorTileLayer enum was created using <a
 * href="https://wiki.openstreetmap.org/wiki/Zoom_levels">OSM ZoomLevels</a> <br>
 * and <a
 * href="https://developers.arcgis.com/documentation/mapping-apis-and-services/reference/zoom-levels-and-scale">ArcGis
 * docs</a>.
 */
@Getter
public enum ArcgisImageZoom {
  WORLD_0(0),
  WORLD_1(1),
  WORLD_2(2),
  CONTINENT_0(3),
  CONTINENT_1(4),
  COUNTRIES(5),
  COUNTRY(6),
  STATES(7),
  COUNTIES_0(8),
  COUNTIES_1(9),
  COUNTY(10),
  METROPOLITAN_AREA(11),
  CITIES(12),
  CITY(13),
  TOWN(14),
  NEIGHBORHOOD(15),
  STREETS(16),
  CITY_BLOCK(17),
  BUILDINGS(18),
  BUILDING(19),
  HOUSES_0(20),
  HOUSES_1(21),
  HOUSES_2(22),
  HOUSE_PROPERTY(23);
  final int zoomLevel;

  ArcgisImageZoom(int zoomLevel) {
    this.zoomLevel = zoomLevel;
  }

  public static ArcgisImageZoom fromZoomLevel(int z) {
    for (var arcgisZoom : ArcgisImageZoom.values()) {
      if (arcgisZoom.getZoomLevel() == z) {
        return arcgisZoom;
      }
    }
    throw new IllegalArgumentException("Unknown zoomLevel: " + z);
  }
}
