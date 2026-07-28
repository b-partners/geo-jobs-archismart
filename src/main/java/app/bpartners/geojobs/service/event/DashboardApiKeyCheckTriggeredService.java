package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.dashboard.component.UserApiKeyType.DASHBOARD;

import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.service.dashboard.UserAccountsApi;
import app.bpartners.geojobs.service.dashboard.component.User;
import app.bpartners.geojobs.service.dashboard.component.UserApiKey;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class DashboardApiKeyCheckTriggeredService
    implements Consumer<DashboardApiKeyCheckTriggered> {
  private static final String FAILURE_LOG_PREFIX = "[DAKC F] ";
  private static final String SUCCESS_LOG_PREFIX = "[DAKC S] ";
  private static final String DASHBOARD_API_KEY_VERIFICATION_TEMPLATE =
      "dashboard_api_key_verification_template";
  public static final String EUROPE_PARIS = "Europe/Paris";
  public static final String EMAIL_NOTIFICATION_RECEIVER = "tech@birdia.fr";
  private final UserAccountsApi userAccountsApi;
  private final String adminApiKey;
  private final Mailer mailer;
  private final HTMLTemplateParser htmlTemplateParser;

  public DashboardApiKeyCheckTriggeredService(
      UserAccountsApi userAccountsApi,
      @Value("${admin.api.key}") String adminApiKey,
      Mailer mailer,
      HTMLTemplateParser htmlTemplateParser) {
    this.userAccountsApi = userAccountsApi;
    this.adminApiKey = adminApiKey;
    this.mailer = mailer;
    this.htmlTemplateParser = htmlTemplateParser;
  }

  @Override
  public void accept(DashboardApiKeyCheckTriggered event) {
    var email = event.getEmail();
    var userId = event.getCommunityAuthorizationId();
    List<String> collectedErrors = new ArrayList<>();

    var retrievedUsers = userAccountsApi.getUsersByCriteria(email, null, null, adminApiKey);
    var retrievedUserIds = retrievedUsers.stream().map(User::id).toList();

    if (retrievedUsers.isEmpty()) {
      var logMessage = "No users with same email as " + userId + " found in user account api.";
      log.warn(FAILURE_LOG_PREFIX + "{}", logMessage);
      return;
    }

    if (retrievedUsers.size() > 1) {
      log.warn(
          "Multiple ({}) account ( {} ) attached to the email of the user {}",
          retrievedUsers.size(),
          String.join(" ", retrievedUserIds),
          userId);
    }

    var userApiKeys =
        retrievedUsers.stream()
            .map(
                user -> {
                  try {
                    return userAccountsApi.getUserApiKey(user.id(), adminApiKey);
                  } catch (RestClientResponseException e) {
                    var exceptionMessage =
                        String.format(
                            "Unable to get api key for user with id : %s in user account api. %s"
                                + " %s",
                            user.id(), e.getStatusCode(), e.getMessage());
                    log.error(FAILURE_LOG_PREFIX + "{}", exceptionMessage, e);
                    collectedErrors.add(exceptionMessage);
                    return new ArrayList<UserApiKey>();
                  }
                })
            .flatMap(List::stream)
            .toList();

    if (userApiKeys.isEmpty()) {
      var exceptionMessage = "No api key found for users : " + retrievedUserIds;
      log.error(FAILURE_LOG_PREFIX + "{}", exceptionMessage);
      return;
    }

    List<String> dashboardApiKeys =
        userApiKeys.stream()
            .filter(apiKey -> DASHBOARD.equals(apiKey.type()))
            .map(UserApiKey::key)
            .toList();

    if (dashboardApiKeys.isEmpty()) {
      var exceptionMessage =
          "No dashboard api key found for users : "
              + retrievedUserIds
              + " in the user account api.";
      log.error(FAILURE_LOG_PREFIX + "{}", exceptionMessage);
      return;
    }

    String actualDashboardApiKey = event.getDashboardApiKey();
    if (!dashboardApiKeys.contains(actualDashboardApiKey)) {
      var exceptionMessage =
          "Actual dashboard api "
              + actualDashboardApiKey
              + " key doesn't match any of the api keys in the users account api for : "
              + formIdList(retrievedUserIds);
      log.error(FAILURE_LOG_PREFIX + "{}", exceptionMessage);
    }

    if (collectedErrors.isEmpty()) {
      log.info(
          SUCCESS_LOG_PREFIX + "Dashboard api key verification for user {} succeeded.", userId);
    }
  }

  @SneakyThrows
  private void notifyByEmail(List<String> idList, List<String> errorMessages) {
    var subject = computeSubject(idList);
    String combinedMessage = String.join("\n", errorMessages);
    String emailBody = computeEmailBody(idList, combinedMessage);

    mailer.accept(
        new Email(
            new InternetAddress(EMAIL_NOTIFICATION_RECEIVER),
            List.of(),
            List.of(),
            subject,
            emailBody,
            List.of()));
  }

  private String computeEmailBody(List<String> idList, String message) {
    Context context = new Context();
    context.setVariable("email", formIdList(idList));
    context.setVariable("message", message);
    return htmlTemplateParser.apply(DASHBOARD_API_KEY_VERIFICATION_TEMPLATE, context);
  }

  private String computeSubject(List<String> idList) {
    var nowInParisHour = ZonedDateTime.now(ZoneId.of(EUROPE_PARIS));
    var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    return "[geo-jobs] Erreur lors de la vérification automatique de la clé API du client "
        + formIdList(idList)
        + " le "
        + formatter.format(nowInParisHour);
  }

  @NotNull
  private static String formIdList(List<String> idList) {
    return String.join("/", idList);
  }
}
