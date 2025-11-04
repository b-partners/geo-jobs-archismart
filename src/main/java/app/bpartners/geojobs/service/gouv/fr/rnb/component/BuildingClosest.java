package app.bpartners.geojobs.service.gouv.fr.rnb.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BuildingClosest(
    @JsonProperty("next") String nextUrl,
    @JsonProperty("previous") String previousUrl,
    List<Building> results,
    Double distance) {}
