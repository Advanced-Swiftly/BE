package com.advancedswiftly.pawfit.publicdata;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
class PublicDataConfiguration {
    @Bean(defaultCandidate = false)
    ThreadPoolTaskExecutor publicDataExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("public-data-");
        return executor;
    }
}
