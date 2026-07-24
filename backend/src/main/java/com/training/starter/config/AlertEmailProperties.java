package com.training.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "stockpulse.alert-email")
public record AlertEmailProperties(
        String from,
        String recipients
) {

    public List<String> recipientList() {
        if (recipients == null) {
            return List.of();
        }
        return Arrays.stream(recipients.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }
}
