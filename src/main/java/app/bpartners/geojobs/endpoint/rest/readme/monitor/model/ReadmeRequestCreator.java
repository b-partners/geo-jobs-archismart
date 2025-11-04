package app.bpartners.geojobs.endpoint.rest.readme.monitor.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import lombok.Builder;

@Builder
@JsonInclude(NON_NULL)
public record ReadmeRequestCreator(String name, String version, String comment)
    implements Serializable {}
