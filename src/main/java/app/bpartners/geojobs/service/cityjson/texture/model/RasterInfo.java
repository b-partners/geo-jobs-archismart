package app.bpartners.geojobs.service.cityjson.texture.model;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

public record RasterInfo(
    double originX,
    double originY,
    double pixelWidth,
    double pixelHeight,
    double shearX,
    double shearY,
    int width,
    int height,
    CoordinateReferenceSystem crs) {}
