package app.bpartners.geojobs.endpoint.rest.readme.monitor.model.entry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import lombok.Builder;

@Builder
@JsonInclude(NON_NULL)
public record ReadmeEntryResponseContent(
    Integer size, String mimeType, String text, String encoding) implements Serializable {}
