package com.advancedswiftly.pawfit.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

class ProdFlywayConfigTest {

    @Test
    void supabase초기스키마를Version0으로Baseline한다() throws IOException {
        var propertySources = new YamlPropertySourceLoader()
                .load("application-prod.yml", new ClassPathResource("application-prod.yml"));

        var properties = propertySources.getFirst();
        assertThat(properties.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo(true);
        assertThat(properties.getProperty("spring.flyway.baseline-version")).isEqualTo(0);
    }
}
