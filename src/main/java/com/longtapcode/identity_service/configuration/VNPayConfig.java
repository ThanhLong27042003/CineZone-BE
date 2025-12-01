package com.longtapcode.identity_service.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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
