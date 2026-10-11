package xyz.mobi.reportservice.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class ReportUserPrincipal implements UserDetails {

    private final Long employeeId;
    private final String role;
    private final String timezone;
    private final String rawToken;
    private final Collection<? extends GrantedAuthority> authorities;

    public ReportUserPrincipal(Long employeeId, String role, String timezone, String rawToken) {
        this.employeeId = employeeId;
        this.role = role != null ? role : "EMPLOYEE";
        this.timezone = timezone != null ? timezone : "UTC";
        this.rawToken = rawToken;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + this.role));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return String.valueOf(employeeId);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
