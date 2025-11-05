package com.pdf.pdfapi.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that ensures environment variables are loaded during application startup.
 * This triggers the loading of the .env file and sets system properties.
 */
@Configuration
public class EnvConfigInitializer {

    public EnvConfigInitializer() {
        // Load environment variables from .env file during Spring initialization
        EnvConfig.load();
    }
}
