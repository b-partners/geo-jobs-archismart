package app.bpartners.geojobs.service.gouv.fr.rnb.component;

import app.bpartners.geojobs.service.gouv.fr.rnb.component.geometry.Geometry;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Building(
    @JsonProperty("rnb_id") String rnbId,
    BuildingStatus status,
    Geometry point,
    Geometry shape,
    List<BuildingAddress> addresses,
    Double distance) {}
