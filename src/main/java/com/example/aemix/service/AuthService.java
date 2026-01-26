package com.example.aemix.service;
import com.example.aemix.dto.requests.AuthRequest;
import com.example.aemix.dto.responses.AuthResponse;
import com.example.aemix.entity.User;
import com.example.aemix.entity.enums.Role;
import com.example.aemix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationService verificationService;

    public String register(AuthRequest request) {
        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        verificationService.sendVerification(user);

        return "We have sent an email verification code. Please check your email.";
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if(!user.getIsVerified()){
            verificationService.sendVerification(user);
            throw new DisabledException(
                    "Email is not verified. Please check your email for the verification message."
            );
        }

        var token = tokenService.generateToken(user);
        return AuthResponse.builder().token(token).build();
    }
}