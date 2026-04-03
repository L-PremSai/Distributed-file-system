package com.medicalstorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Medical Image Storage System.
 * Bootstraps the Spring Boot context and starts the embedded Tomcat server.
 */
@SpringBootApplication
public class MedicalStorageApplication {
    public static void main(String[] args) {
        SpringApplication.run(MedicalStorageApplication.class, args);
    }
}
