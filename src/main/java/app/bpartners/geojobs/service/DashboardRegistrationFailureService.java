package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationStatus.PENDING;
import static app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationStatus.REPROCESSED;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionTrackingRegistrationRequested;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.endpoint.event.model.ThreeDTrackingRegistrationRequested;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.DashboardRegistrationFailureRepository;
import app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationFailure;
import app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationType;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

@Slf4j
@Service
public class DashboardRegistrationFailureService {
  private static final String TEMPLATE = "dashboard_registration_failed";
  private static final ObjectMapper PAYLOAD_MAPPER = objectMapper().findAndRegisterModules();
  private final DashboardRegistrationFailureRepository repository;
  private final EventProducer eventProducer;
  private final Mailer mailer;
  private final HTMLTemplateParser htmlTemplateParser;
  private final String adminEmail;
  private final String env = System.getenv("ENV");

  public DashboardRegistrationFailureService(
      DashboardRegistrationFailureRepository repository,
      EventProducer eventProducer,
      Mailer mailer,
      HTMLTemplateParser htmlTemplateParser,
      @Value("${admin.email}") String adminEmail) {
    this.repository = repository;
    this.eventProducer = eventProducer;
    this.mailer = mailer;
    this.htmlTemplateParser = htmlTemplateParser;
    this.adminEmail = adminEmail;
  }

  @Transactional
  public DashboardRegistrationFailure recordFailure(
      DashboardRegistrationType type,
      String subjectId,
      String communityOwnerId,
      String projectKey,
      Object payload,
      Exception cause) {
    var existing = repository.findFirstByTypeAndSubjectIdAndStatus(type, subjectId, PENDING);
    var failure =
        existing
            .map(
                pending ->
                    pending.toBuilder()
                        .projectKey(projectKey)
                        .payload(serialize(payload))
                        .errorMessage(messageOf(cause))
                        .attempts(pending.getAttempts() + 1)
                        .lastAttemptDatetime(now())
                        .build())
            .orElseGet(
                () ->
                    DashboardRegistrationFailure.builder()
                        .id(randomUUID().toString())
                        .type(type)
                        .subjectId(subjectId)
                        .communityOwnerId(communityOwnerId)
                        .projectKey(projectKey)
                        .payload(serialize(payload))
                        .errorMessage(messageOf(cause))
                        .attempts(1)
                        .status(PENDING)
                        .creationDatetime(now())
                        .lastAttemptDatetime(now())
                        .build());
    var saved = repository.save(failure);
    notifyAdmin(saved);
    return saved;
  }

  @Transactional
  public void resolve(String failureId) {
    repository
        .findById(failureId)
        .filter(failure -> PENDING.equals(failure.getStatus()))
        .ifPresent(
            pending ->
                repository.save(
                    pending.toBuilder().status(REPROCESSED).reprocessedDatetime(now()).build()));
  }

  public DashboardRegistrationFailure reprocess(String id) {
    var failure =
        repository
            .findById(id)
            .orElseThrow(
                () ->
                    new NotFoundException("DashboardRegistrationFailure(id=" + id + ") not found"));
    if (!PENDING.equals(failure.getStatus())) {
      throw new BadRequestException(
          "DashboardRegistrationFailure(id=" + id + ") is already " + failure.getStatus());
    }
    eventProducer.accept(List.of(eventOf(failure)));
    return failure;
  }

  private static PojaEvent eventOf(DashboardRegistrationFailure failure) {
    return switch (failure.getType()) {
      case DETECTION_2D ->
          new DetectionTrackingRegistrationRequested(failure.getSubjectId(), failure.getId());
      case THREE_D_3D ->
          new ThreeDTrackingRegistrationRequested(
              failure.getSubjectId(), failure.getCommunityOwnerId(), failure.getId());
    };
  }

  private void notifyAdmin(DashboardRegistrationFailure failure) {
    try {
      var context = new Context();
      context.setVariable("failure", failure);
      context.setVariable("env", env);
      mailer.accept(
          new Email(
              new InternetAddress(adminEmail),
              List.of(),
              List.of(),
              "[geo-jobs/"
                  + env
                  + "] bpartners-api injoignable, enregistrement à rejouer ("
                  + failure.getType()
                  + ")",
              htmlTemplateParser.apply(TEMPLATE, context),
              List.of()));
    } catch (Exception e) {
      log.error(
          "Unable to notify admin about DashboardRegistrationFailure(id={})", failure.getId(), e);
    }
  }

  private static String serialize(Object payload) {
    try {
      return PAYLOAD_MAPPER.writeValueAsString(payload);
    } catch (Exception e) {
      return String.valueOf(payload);
    }
  }

  private static String messageOf(Exception cause) {
    var message = cause.getMessage();
    return message == null || message.length() <= 1000 ? message : message.substring(0, 1000);
  }
}
