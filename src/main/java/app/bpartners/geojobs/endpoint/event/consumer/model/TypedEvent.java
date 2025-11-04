package app.bpartners.geojobs.endpoint.event.consumer.model;

import app.bpartners.geojobs.PojaGenerated;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;

@PojaGenerated
@SuppressWarnings("all")
public record TypedEvent(String typeName, PojaEvent payload) {}
