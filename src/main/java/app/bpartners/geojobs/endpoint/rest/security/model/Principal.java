package app.bpartners.geojobs.endpoint.rest.security.model;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.*;

import app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@ToString
@AllArgsConstructor
public class Principal implements UserDetails {
  private final String apiKey;
  private Collection<? extends GrantedAuthority> authorities;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return apiKey;
  }

  @Override
  public String getUsername() {
    return null;
  }

  @Override
  public boolean isAccountNonExpired() {
    return isEnabled();
  }

  @Override
  public boolean isAccountNonLocked() {
    return isEnabled();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return isEnabled();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  public List<Role> getRoles() {
    return authorities.stream()
        .map(grantedAuthority -> Role.valueOf(grantedAuthority.getAuthority()))
        .toList();
  }

  public boolean isAdmin() {
    return getRoles().contains(ROLE_ADMIN);
  }

  public boolean isInsurance() {
    return getRoles().contains(ROLE_INSURANCE);
  }

  public boolean isCommunity() {
    return getRoles().contains(ROLE_COMMUNITY);
  }
}
