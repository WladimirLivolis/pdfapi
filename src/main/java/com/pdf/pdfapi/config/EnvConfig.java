package com.pdf.pdfapi.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.log4j.Log4j2;

import java.io.File;

/**
 * Utility class to load environment variables from .env file
 * This class loads the .env file during application startup and sets environment variables
 */
@Log4j2
public final class EnvConfig {

    /**
     * Private constructor to hide the implicit public one.
     * This class should not be instantiated.
     */
    private EnvConfig() {
        throw new AssertionError("Cannot instantiate EnvConfig");
    }

    /**
     * Loads environment variables from .env file.
     * This method is called during application startup to ensure credentials are available.
     */
    public static void load() {
        String envPath = findEnvFile();

        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()  // Don't fail if .env file doesn't exist
                .directory(envPath)
                .filename(".env")
                .load();

        // Set specific system properties from .env file so Spring can use them
        setPropertyIfExists(dotenv, "SECURITY_USER_USERNAME");
        setPropertyIfExists(dotenv, "SECURITY_USER_PASSWORD");
        setPropertyIfExists(dotenv, "SECURITY_ADMIN_USERNAME");
        setPropertyIfExists(dotenv, "SECURITY_ADMIN_PASSWORD");
        setPropertyIfExists(dotenv, "PDF_OUTPUT_FOLDER");
    }

    /**
     * Finds the correct directory for the .env file.
     * Checks multiple locations: current directory, user home directory, and project root.
     */
    private static String findEnvFile() {
        String[] potentialPaths = {
            ".",  // Current working directory (where app is launched from)
            System.getProperty("user.dir"),  // Current working directory (explicit)
            System.getProperty("user.home")  // Home directory as fallback
        };

        for (String path : potentialPaths) {
            File envFile = new File(path, ".env");
            if (envFile.exists()) {
                log.info("Found .env file at: {}", envFile.getAbsolutePath());
                return path;
            }
        }

        log.info("WARNING: .env file not found in: {}", String.join(", ", potentialPaths));
        log.info("Using current directory: {}", System.getProperty("user.dir"));
        return ".";
    }

    private static void setPropertyIfExists(Dotenv dotenv, String key) {
        String value = dotenv.get(key);
        if (value != null && !value.isEmpty()) {
            System.setProperty(key, value);
            log.info("Loaded environment variable: {}", key);
        } else {
            log.info("WARNING: Environment variable not found: {}", key);
        }
    }
}
