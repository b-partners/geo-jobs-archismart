package app.bpartners.geojobs.endpoint.rest.readme.webhook.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import lombok.Builder;

@Builder
@JsonInclude(NON_NULL)
public record SingleUserInfo(String apiKey, String name, String email, Boolean isAdmin)
    implements Serializable {}
