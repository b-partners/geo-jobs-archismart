package app.bpartners.geojobs.readme.webhook.model;

import java.io.Serializable;
import lombok.Builder;

@Builder
public record CreateWebhook(String email, String readmeProject) implements Serializable {}
