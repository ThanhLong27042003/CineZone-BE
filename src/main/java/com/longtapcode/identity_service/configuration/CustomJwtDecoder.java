package com.longtapcode.identity_service.configuration;

import java.text.ParseException;
import java.time.Duration;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import com.longtapcode.identity_service.dto.request.VerifyRequest;
import com.longtapcode.identity_service.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomJwtDecoder implements JwtDecoder {
    @NonFinal
    @Value("${jwt.signerKey}")
    protected String signerKey;

    AuthenticationService authenticationService;

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            authenticationService.verifyToken(
                    token, false);
        } catch (JOSEException | ParseException e) {
            throw new JwtException(e.getMessage());
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
        NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();

        JwtTimestampValidator jwtTimestampValidator = new JwtTimestampValidator(Duration.ofSeconds(0));
        nimbusJwtDecoder.setJwtValidator(jwtTimestampValidator);
        return nimbusJwtDecoder.decode(token);
    }
}
