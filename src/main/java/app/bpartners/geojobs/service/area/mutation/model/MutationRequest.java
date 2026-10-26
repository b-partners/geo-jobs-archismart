package app.bpartners.geojobs.service.area.mutation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public record MutationRequest(
    @JsonProperty("base64_old") String base64Old,
    @JsonProperty("base64_new") String base64New,
    @JsonProperty("base64_mask") String base64Mask,
    @JsonProperty("filename") String filename)
    implements Serializable {}
