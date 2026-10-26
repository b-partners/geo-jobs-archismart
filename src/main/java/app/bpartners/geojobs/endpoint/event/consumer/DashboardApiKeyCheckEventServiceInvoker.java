package app.bpartners.geojobs.endpoint.event.consumer;

import app.bpartners.geojobs.endpoint.event.consumer.model.TypedEvent;
import app.bpartners.geojobs.endpoint.event.model.AllDashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.service.event.AllDashboardApiKeyCheckTriggeredService;
import app.bpartners.geojobs.service.event.DashboardApiKeyCheckTriggeredService;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Same as {@link EventServiceInvoker} but skips its ~10s per event classpath scan for the dashboard
 * api key check events, other events fall back to it.
 */
@Primary
@Component
public class DashboardApiKeyCheckEventServiceInvoker extends EventServiceInvoker {
  private static final Map<String, Class<? extends Consumer<?>>> SERVICES =
      Map.of(
          DashboardApiKeyCheckTriggered.class.getTypeName(),
          DashboardApiKeyCheckTriggeredService.class,
          AllDashboardApiKeyCheckTriggered.class.getTypeName(),
          AllDashboardApiKeyCheckTriggeredService.class);

  private final ApplicationContext applicationContext;

  public DashboardApiKeyCheckEventServiceInvoker(ApplicationContext applicationContext) {
    super(applicationContext);
    this.applicationContext = applicationContext;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void accept(TypedEvent typedEvent) {
    var service = SERVICES.get(typedEvent.typeName());
    if (service == null) {
      super.accept(typedEvent);
      return;
    }
    ((Consumer<PojaEvent>) applicationContext.getBean(service)).accept(typedEvent.payload());
  }
}
