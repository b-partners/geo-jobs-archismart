package app.bpartners.geojobs.model.geometry.area.rate;

import app.bpartners.geojobs.repository.model.detection.RoofCoveringType;
import app.bpartners.geojobs.service.area.mutation.model.MutationType;

/**
 * vegetationFeu peut rester une entree explicite du contrat, ou etre derive du module de detection
 * vegetation/risque feu ({@code RoofAssessmentFacade}/{@code FireRiskEvaluator}) quand ce dernier a
 * pu etre calcule. Tant que ce module n'est pas juge fiable, ou lorsqu'il n'a pas pu etre execute,
 * la valeur derivee doit rester null plutot que d'imposer un malus non fiable (voir
 * FeatureRoofResultPropertiesComputer).
 */
public record RoofDamageRates(
    Double humiditeRate,
    Double usureRate,
    Double moisissureRate,
    RoofCoveringType revetement,
    MutationType monitoring,
    Double penteDegres,
    Boolean vegetationFeu) {

  public RoofDamageRates(Double humiditeRate, Double usureRate, Double moisissureRate) {
    this(humiditeRate, usureRate, moisissureRate, null, null, null, null);
  }
}
