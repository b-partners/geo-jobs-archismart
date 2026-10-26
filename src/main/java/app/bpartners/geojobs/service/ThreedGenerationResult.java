package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import java.util.List;

/**
 * Result of {@link CityJSONThreedProcessor#applyWithDiagnostics}: the generated CityJSON resources,
 * plus the message and warnings threed reported for any building that generated as DEGRADED (both
 * null/empty when every building generated as NORMAL).
 */
public record ThreedGenerationResult(
    List<CityJSON> cityJsons, String degradedMessage, List<String> warnings) {}
