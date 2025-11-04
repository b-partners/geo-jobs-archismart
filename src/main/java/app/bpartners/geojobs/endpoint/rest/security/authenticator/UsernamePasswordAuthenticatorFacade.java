package app.bpartners.geojobs.endpoint.rest.security.authenticator;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Primary
@Component
@AllArgsConstructor
public class UsernamePasswordAuthenticatorFacade implements UsernamePasswordAuthenticator {
  private final ApiKeyAuthenticator authenticator;

  @Override
  public UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken authentication) {
    // add more authenticator here
    return authenticator.retrieveUser(username, authentication);
  }
}
