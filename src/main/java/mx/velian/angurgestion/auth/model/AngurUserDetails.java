package mx.velian.angurgestion.auth.model;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class AngurUserDetails implements UserDetails {
    private final UserAccount account;

    public AngurUserDetails(UserAccount account) {
        this.account = account;
    }

    public String getDisplayName() {
        return account.name();
    }

    public String getRoleLabel() {
        return account.role().getLabel();
    }

    public UserRole getRole() {
        return account.role();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name()));
    }

    @Override
    public String getPassword() {
        return account.passwordHash();
    }

    @Override
    public String getUsername() {
        return account.username();
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
        return account.active();
    }
}
