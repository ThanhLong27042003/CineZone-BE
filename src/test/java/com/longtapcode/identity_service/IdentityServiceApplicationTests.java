package com.longtapcode.identity_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Disabled("Requires database and Kafka to be running")
@ActiveProfiles("local")
class IdentityServiceApplicationTests {

    @Test
    void contextLoads() {}
}
