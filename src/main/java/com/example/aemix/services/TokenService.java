package com.example.aemix.services;

import com.example.aemix.config.JwtConfig;
import com.example.aemix.entities.User;
import com.example.aemix.exceptions.TokenGenerationException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
@AllArgsConstructor
public class TokenService {

    private final JwtConfig jwtConfig;

    public String generateToken(User user) {
        var header = new JWSHeader.Builder(jwtConfig.getAlgorithm())
                .type(JOSEObjectType.JWT)
                .build();

        Instant now = Instant.now();

        var claims = new JWTClaimsSet.Builder()
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusMillis(jwtConfig.getJwtExpiration())))
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .build();

        var jwt = new SignedJWT(header, claims);

        try {
            var signer = new MACSigner(jwtConfig.getSecretKey());
            jwt.sign(signer);
        } catch (JOSEException e) {
            throw new TokenGenerationException("Error generating JWT", e);
        }

        return jwt.serialize();
    }
}
