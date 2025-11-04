package app.bpartners.geojobs.model.geometry.route;

public record ContinuationConf(
    double minDirectionThreshold, double maxDirectionThreshold, double distanceThreshold) {}
