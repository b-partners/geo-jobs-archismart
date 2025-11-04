package app.bpartners.geojobs.model.geometry.area;

import app.bpartners.geojobs.model.ArcgisRasterZoom;
import java.util.function.BiPredicate;
import org.locationtech.jts.geom.Geometry;

public class IsAreaOfParcel implements BiPredicate<Geometry, ArcgisRasterZoom> {

  private final ArcgisRasterZoom referenceZoom;
  private final SquareDegree maxParcelAreaAtReferenceZoom;

  private final AreaComputer areaComputer = new AreaComputer();

  public IsAreaOfParcel(ArcgisRasterZoom referenceZoom, SquareDegree maxParcelAreaAtReferenceZoom) {
    this.referenceZoom = referenceZoom;
    this.maxParcelAreaAtReferenceZoom = maxParcelAreaAtReferenceZoom;
  }

  @Override
  public boolean test(Geometry geometry, ArcgisRasterZoom arcgisRasterZoom) {
    var maxParcelArea =
        new SquareDegree(
            maxParcelAreaAtReferenceZoom.getValue()
                * referenceZoom.nbImagesMultiplierTo(arcgisRasterZoom));
    return areaComputer.apply(geometry).compareTo(maxParcelArea) < 0;
  }
}
