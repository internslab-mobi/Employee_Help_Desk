package xyz.mobi.employeehelpdesk.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

import java.util.Collection;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long employeeId;
    private final UserRole role;
    private final String timezone;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(
            Long employeeId,
            UserRole role,
            String timezone,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.employeeId = employeeId;
        this.role = role;
        this.timezone = (timezone != null && !timezone.isBlank()) ? timezone.trim() : "UTC";
        this.authorities = authorities;
    }

    @Override
    public String getUsername() {
        return String.valueOf(employeeId);
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}