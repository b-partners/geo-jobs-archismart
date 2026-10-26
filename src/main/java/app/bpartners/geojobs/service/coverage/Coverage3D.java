package app.bpartners.geojobs.service.coverage;

public record Coverage3D(
    boolean covered,
    String region,
    String crs,
    int fileCount,
    int coveredGeometryCount,
    int geometryCount) {}
