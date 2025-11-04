package app.bpartners.geojobs.endpoint.rest.readme.webhook.model;

import java.io.Serializable;
import lombok.Builder;

@Builder
public record SignatureWithTime(String signature, long time) implements Serializable {}
