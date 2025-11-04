package app.bpartners.geojobs.endpoint.rest.readme.monitor.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Builder;

@Builder
@JsonInclude(NON_NULL)
public record ReadmeGroup(@JsonProperty("id") String apiKey, String email, String label)
    implements Serializable {}
