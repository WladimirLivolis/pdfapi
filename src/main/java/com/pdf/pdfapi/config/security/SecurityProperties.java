package com.pdf.pdfapi.config.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "security")
@Validated
@Data
public class SecurityProperties {

    private Map<String, UserConfig> users;

    @Data
    public static class UserConfig {
        @NotBlank(message = "Username cannot be blank")
        private String username;

        @NotBlank(message = "Password cannot be blank")
        private String password;

        @NotBlank(message = "Roles cannot be blank")
        private String roles;
    }
}
