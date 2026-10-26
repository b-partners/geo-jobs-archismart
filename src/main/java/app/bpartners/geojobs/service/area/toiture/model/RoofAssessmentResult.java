package app.bpartners.geojobs.service.area.toiture.model;

public record RoofAssessmentResult(
    VegetationIndex vegetationIndex,
    FireRiskLevel fireRiskLevel,
    MaintenancePriority maintenancePriority) {}
