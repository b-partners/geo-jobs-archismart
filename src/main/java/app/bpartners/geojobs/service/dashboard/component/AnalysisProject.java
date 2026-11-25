package app.bpartners.geojobs.service.dashboard.component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalysisProject(
    String projectKey,
    boolean analysis2dDone,
    boolean model3dDone,
    boolean creditConsumed,
    List<String> remainingSteps,
    Long creditCost,
    Boolean creditsSufficient) {}
