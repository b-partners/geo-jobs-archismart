package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.dashboard.component.UserApiKeyType.DASHBOARD;

import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.service.dashboard.UserAccountsApi;
import app.bpartners.geojobs.service.dashboard.component.User;
import app.bpartners.geojobs.service.dashboard.component.UserApiKey;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class DashboardApiKeyCheckTriggeredService
    implements Consumer<DashboardApiKeyCheckTriggered> {
  private static final String DASHBOARD_API_KEY_VERIFICATION_TEMPLATE =
      "dashboard_api_key_verification_template";
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
    var authorization = event.getCommunityAuthorization();

    var retrievedUsers =
        userAccountsApi.getUsersByCriteria(authorization.getEmail(), null, null, adminApiKey);

    var retrievedUserEmails = retrievedUsers.stream().map(User::email).toList();

    if (retrievedUsers.isEmpty()) {
      log.info("Users with email {} not found in user accounts api.", authorization.getEmail());
      return;
    }

    if (retrievedUsers.size() > 1) {
      log.warn(
          "Multiple ({}) account attached to the email : {} in user account api",
          retrievedUsers.size(),
          authorization.getEmail());
    }

    var userApiKeys =
        retrievedUsers.stream()
            .map(
                user -> {
                  try {
                    return userAccountsApi.getUserApiKey(user.id(), adminApiKey);
                  } catch (RestClientResponseException e) {
                    log.warn(
                        "Unable to get api key for user with id : {} in user account api",
                        user.id());
                    return new ArrayList<UserApiKey>();
                  }
                })
            .flatMap(List::stream)
            .toList();

    if (userApiKeys.isEmpty()) {
      var exceptionMessage = "No api found for users with email : " + retrievedUserEmails;
      notifyByEmail(retrievedUserEmails, exceptionMessage);
      return;
    }

    List<String> dashboardApiKeys =
        userApiKeys.stream()
            .filter(apiKey -> DASHBOARD.equals(apiKey.type()))
            .map(UserApiKey::key)
            .toList();

    if (dashboardApiKeys.isEmpty()) {
      var exceptionMessage =
          "No dashboard api key found for users with email : "
              + retrievedUserEmails
              + "in the user account api.";
      notifyByEmail(retrievedUserEmails, exceptionMessage);
      return;
    }

    String actualDashboardApiKey = authorization.getDashboardApiKey();
    if (!dashboardApiKeys.contains(actualDashboardApiKey)) {
      var exceptionMessage =
          "Actual dashboard api "
              + actualDashboardApiKey
              + " key doesn't match any of the api keys in the users account api for : "
              + retrievedUserEmails;
      notifyByEmail(retrievedUserEmails, exceptionMessage);
      return;
    }
  }

  @SneakyThrows
  private void notifyByEmail(List<String> emailList, String message) {
    var subject = computeSubject(emailList);
    String emailBody = computeEmailBody(emailList, message);

    mailer.accept(
        new Email(
            new InternetAddress("tech@birdia.fr"),
            List.of(),
            List.of(),
            subject,
            emailBody,
            List.of()));
  }

  private String computeEmailBody(List<String> emailList, String message) {
    Context context = new Context();
    context.setVariable("email", emailList);
    context.setVariable("message", message);
    return htmlTemplateParser.apply(DASHBOARD_API_KEY_VERIFICATION_TEMPLATE, context);
  }

  private String computeSubject(List<String> emailList) {
    var nowInParisHour = LocalDateTime.now(ZoneId.of("Europe/Paris"));
    var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    return "[geo-jobs] Erreur lors de la vérification automatique de la clé API du client "
        + emailList
        + " le "
        + formatter.format(nowInParisHour);
  }
}
