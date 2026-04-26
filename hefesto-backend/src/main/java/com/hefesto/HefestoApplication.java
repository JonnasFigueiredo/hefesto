package com.hefesto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.hefesto")
public class HefestoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HefestoApplication.class, args);
    }
}
