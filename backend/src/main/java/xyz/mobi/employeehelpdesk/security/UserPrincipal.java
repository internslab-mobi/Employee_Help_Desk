package xyz.mobi.employeehelpdesk.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long employeeId;
    private final String email;
    private final String password;
    private final UserRole role;
    private final String timezone;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(
            Long employeeId,
            String email,
            String password,
            UserRole role,
            String timezone,
            boolean enabled,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.employeeId = employeeId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.timezone = (timezone != null && !timezone.isBlank()) ? timezone.trim() : "UTC";
        this.enabled = enabled;
        this.authorities = authorities;
    }

    public UserPrincipal(
            Long employeeId,
            String email,
            String password,
            UserRole role,
            boolean enabled,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this(employeeId, email, password, role, "UTC", enabled, authorities);
    }

    public static UserPrincipal create(Employee employee) {
        String roleName = employee.getRole() != null ? employee.getRole().name() : UserRole.EMPLOYEE.name();
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + roleName)
        );

        boolean isEligible = employee.getEmploymentStatus() == EmploymentStatus.ACTIVE;

        return new UserPrincipal(
                employee.getId(),
                employee.getEmail(),
                employee.getPasswordHash(),
                employee.getRole() != null ? employee.getRole() : UserRole.EMPLOYEE,
                employee.getTimezone() != null ? employee.getTimezone() : "UTC",
                isEligible,
                authorities
        );
    }

    public java.time.ZoneId getZoneId() {
        return java.time.ZoneId.of(this.timezone);
    }

    @Override
    public String getUsername() {
        return email;
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
        return enabled;
    }
}
