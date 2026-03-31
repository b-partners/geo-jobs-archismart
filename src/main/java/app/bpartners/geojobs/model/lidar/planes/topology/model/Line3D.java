package app.bpartners.geojobs.model.lidar.planes.topology.model;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import org.locationtech.jts.math.Vector3D;

public record Line3D(LasPointGeometry point, Vector3D direction) {}
