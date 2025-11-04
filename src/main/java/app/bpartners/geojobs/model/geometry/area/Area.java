package app.bpartners.geojobs.model.geometry.area;

public abstract sealed class Area implements Comparable<Area> permits MetricArea, SquareDegree {}
