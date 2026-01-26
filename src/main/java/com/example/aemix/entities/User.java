package com.example.aemix.entities;

import com.example.aemix.entities.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue
    private Integer id;

    @Column(unique = true, nullable = false )
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "telegram_id", unique = true)
    private Long telegramId;

    @Column(name = "telegram_username")
    private String telegramUsername;

    @Column(name = "telegram_first_name")
    private String telegramFirstName;

    @Column(name = "telegram_last_name")
    private String telegramLastName;

    @Column(name = "telegram_photo_url")
    private String telegramPhotoUrl;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_expiration")
    private LocalDateTime verificationExpiresAt;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Boolean isVerified;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @PrePersist
    public void prePersist() {
        if (isVerified == null) {
            isVerified = false;
        }
    }
}