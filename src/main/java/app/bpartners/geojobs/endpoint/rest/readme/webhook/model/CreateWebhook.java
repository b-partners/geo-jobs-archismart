package app.bpartners.geojobs.endpoint.rest.readme.webhook.model;

import java.io.Serializable;
import lombok.Builder;

@Builder
public record CreateWebhook(String email, String readmeProject) implements Serializable {}
