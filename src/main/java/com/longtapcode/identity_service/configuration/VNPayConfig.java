package com.longtapcode.identity_service.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Configuration
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VNPayConfig {

    @Value("${vnpay.pay-url}")
    String payUrl;

    @Value("${vnpay.return-url}")
    String returnUrl;

    @Value("${vnpay.tmn-code}")
    String tmnCode;

    @Value("${vnpay.secret-key}")
    String secretKey;
}
