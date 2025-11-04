package app.bpartners.geojobs.endpoint.rest.security.authenticator;

import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import java.util.HashSet;
import java.util.Objects;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyAuthenticator implements UsernamePasswordAuthenticator {
  public static final String API_KEY_HEADER = "x-api-key";
  private final CommunityAuthorizationRepository caRepository;

  public ApiKeyAuthenticator(CommunityAuthorizationRepository communityAuthorizationRepository) {
    this.caRepository = communityAuthorizationRepository;
  }

  @Override
  public UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    String candidateApiKey = getApiKeyFromHeader(usernamePasswordAuthenticationToken);
    HashSet<Authority> authorities = getAuthorities(candidateApiKey);
    if (!authorities.isEmpty()) {
      return new Principal(candidateApiKey, authorities);
    }
    throw new BadCredentialsException("Bad credentials");
  }

  private HashSet<Authority> getAuthorities(String candidateApiKey) {
    HashSet<Authority> authorities = new HashSet<>();
    var authenticatedUser =
        caRepository
            .findByApiKey(candidateApiKey)
            .filter(authorization -> !authorization.isApiKeyRevoked())
            .orElseThrow(() -> new BadCredentialsException("Bad credentials"));
    authorities.add(new Authority(authenticatedUser.getRole()));
    return authorities;
  }

  private String getApiKeyFromHeader(
      UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    Object tokenObject = usernamePasswordAuthenticationToken.getCredentials();
    if (!(tokenObject instanceof String)
        || !Objects.equals(usernamePasswordAuthenticationToken.getName(), API_KEY_HEADER)) {
      return null;
    }
    return ((String) tokenObject);
  }
}
