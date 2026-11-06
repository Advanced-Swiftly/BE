package com.advancedswiftly.pawfit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class PawfitApplication {

    public static void main(String[] args) {
        SpringApplication.run(PawfitApplication.class, args);
    }
}
