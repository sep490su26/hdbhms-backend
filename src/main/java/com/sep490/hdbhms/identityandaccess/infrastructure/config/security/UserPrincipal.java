package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserPrincipal implements OAuth2User, OidcUser, UserDetails {
    @Getter
    Long id;
    @Getter
    transient String email;
    transient String password;

    @Getter
    AccountStatus accountStatus;
    @Getter
    boolean mustChangePassword;

    transient Collection<? extends GrantedAuthority> authorities;

    @Getter
    transient Role role;
    @Setter
    transient Map<String, Object> attributes;

    OidcIdToken idToken;
    OidcUserInfo userInfo;

    public static UserPrincipal createFromBasicUser(User userEntity) {
        var authority = new SimpleGrantedAuthority(String.format("ROLE_%s", userEntity.getRole().name()));

        return UserPrincipal.builder()
                .id(userEntity.getId())
                .email(userEntity.getEmail())
                .password(userEntity.getPasswordHash())
                .role(userEntity.getRole())
                .mustChangePassword(userEntity.isMustChangePassword())
                .accountStatus(userEntity.getStatus())
                .authorities(Set.of(authority))
                .build();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }

    @Override
    public Map<String, Object> getClaims() {
        return attributes;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }
}
