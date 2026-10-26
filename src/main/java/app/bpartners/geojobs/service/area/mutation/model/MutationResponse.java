package app.bpartners.geojobs.service.area.mutation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public record MutationResponse(
    @JsonProperty("status") MutationResponseStatus status,
    @JsonProperty("mutation") MutationType mutation,
    @JsonProperty("filename") String filename)
    implements Serializable {}
