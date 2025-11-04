package app.bpartners.geojobs.endpoint.rest.readme.monitor.model.entry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;
import lombok.Builder;

@Builder
@JsonInclude(NON_NULL)
public record ReadmeEntry(
    @JsonProperty("pageref") String pareRef,
    Long time,
    Instant startedDateTime,
    ReadmeEntryRequest request,
    ReadmeEntryResponse response)
    implements Serializable {}
