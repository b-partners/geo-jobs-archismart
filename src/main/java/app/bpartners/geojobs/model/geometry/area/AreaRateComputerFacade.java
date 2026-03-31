package app.bpartners.geojobs.model.geometry.area;

import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import java.util.Collection;
import org.locationtech.jts.geom.Geometry;

public class AreaRateComputerFacade {
  private final HumiditeAreaRateComputer humiditeRateComputer;
  private final UsureAreaRateComputer usureRateComputer;
  private final MoisissureAreaRateComputer moisissureRateComputer;
  private final RoofScoreComputer roofScoreComputer = new RoofScoreComputer();

  public AreaRateComputerFacade(Geometry roofGeometry, DetectedTile tile) {
    this.humiditeRateComputer = new HumiditeAreaRateComputer(roofGeometry.getArea(), tile);
    this.usureRateComputer = new UsureAreaRateComputer(roofGeometry.getArea(), tile);
    this.moisissureRateComputer = new MoisissureAreaRateComputer(roofGeometry.getArea(), tile);
  }

  public AreaRateComputerFacade(
      Geometry roofGeometry, Collection<PolygonObjectType> polygonObjectTypes) {
    this.humiditeRateComputer =
        new HumiditeAreaRateComputer(roofGeometry.getArea(), polygonObjectTypes);
    this.usureRateComputer = new UsureAreaRateComputer(roofGeometry.getArea(), polygonObjectTypes);
    this.moisissureRateComputer =
        new MoisissureAreaRateComputer(roofGeometry.getArea(), polygonObjectTypes);
  }

  public double getUsureAreaRate() {
    return format(usureRateComputer.getUsureAreaRate());
  }

  public double getMoisissureAreaRate() {
    return format(moisissureRateComputer.getMoisissureAreaRate());
  }

  public double getHumidityAreaRate() {
    return format(humiditeRateComputer.getHumidityAreaRate());
  }

  public double getGlobalRate() {
    return format(
        humiditeRateComputer.getGlobalRate()
            + usureRateComputer.getGlobalRate()
            + moisissureRateComputer.getGlobalRate());
  }

  public Rate getRate() {
    return roofScoreComputer.getRate(getGlobalRate());
  }

  public static double format(double value) {
    return Math.round((value * 100)) / 100.0;
  }
}
