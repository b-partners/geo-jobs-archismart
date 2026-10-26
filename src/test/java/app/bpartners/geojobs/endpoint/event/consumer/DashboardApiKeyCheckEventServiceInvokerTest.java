package app.bpartners.geojobs.endpoint.event.consumer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.concurrency.Workers;
import app.bpartners.geojobs.endpoint.event.consumer.model.TypedEvent;
import app.bpartners.geojobs.endpoint.event.model.AllDashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.service.event.AllDashboardApiKeyCheckTriggeredService;
import app.bpartners.geojobs.service.event.DashboardApiKeyCheckTriggeredService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

class DashboardApiKeyCheckEventServiceInvokerTest {
  ApplicationContext applicationContext = mock();
  DashboardApiKeyCheckEventServiceInvoker subject =
      new DashboardApiKeyCheckEventServiceInvoker(applicationContext);

  @Test
  void accept_invokes_check_service() {
    var service = mock(DashboardApiKeyCheckTriggeredService.class);
    when(applicationContext.getBean(DashboardApiKeyCheckTriggeredService.class))
        .thenReturn(service);
    var event = mock(DashboardApiKeyCheckTriggered.class);

    subject.accept(new TypedEvent(DashboardApiKeyCheckTriggered.class.getTypeName(), event));

    verify(service).accept(event);
  }

  @Test
  void accept_invokes_all_check_service() {
    var service = mock(AllDashboardApiKeyCheckTriggeredService.class);
    when(applicationContext.getBean(AllDashboardApiKeyCheckTriggeredService.class))
        .thenReturn(service);
    var event = mock(AllDashboardApiKeyCheckTriggered.class);

    subject.accept(new TypedEvent(AllDashboardApiKeyCheckTriggered.class.getTypeName(), event));

    verify(service).accept(event);
  }

  @Test
  void event_consumer_gets_the_dedicated_invoker_injected() {
    try (var context =
        new AnnotationConfigApplicationContext(
            Workers.class,
            EventServiceInvoker.class,
            DashboardApiKeyCheckEventServiceInvoker.class,
            EventConsumer.class)) {
      var consumer = context.getBean(EventConsumer.class);

      assertInstanceOf(
          DashboardApiKeyCheckEventServiceInvoker.class,
          ReflectionTestUtils.getField(consumer, "eventServiceInvoker"));
    }
  }
}
