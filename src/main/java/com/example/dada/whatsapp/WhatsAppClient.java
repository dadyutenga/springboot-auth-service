package com.example.dada.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Lightweight HTTP client that wraps calls to the WhatsApp Cloud API.
 */
@Slf4j
@Component
public class WhatsAppClient {

    private final WebClient webClient;
    private final String phoneNumberId;
    private final boolean configured;

    public WhatsAppClient(WebClient.Builder builder,
                          @Value("${whatsapp.api-base-url}") String apiBaseUrl,
                          @Value("${whatsapp.access-token:}") String accessToken,
                          @Value("${whatsapp.phone-number-id:}") String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
        this.configured = accessToken != null && !accessToken.isBlank() &&
                phoneNumberId != null && !phoneNumberId.isBlank();
        if (!configured) {
            log.warn("WhatsApp API credentials are not fully configured; messages will be logged only.");
        }
        this.webClient = builder
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
    }

    /**
     * Send a text message via the WhatsApp Cloud API.
     *
     * <pre>{
     *   "messaging_product": "whatsapp",
     *   "to": "254700000000",
     *   "type": "text",
     *   "text": {"body": "Your delivery is on the way!"}
     * }</pre>
     *
     * @param to      recipient phone number in international format
     * @param message textual body to deliver
     */
    public Mono<Void> sendTextMessage(String to, String message) {
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", message)
        );
        return postPayload(payload);
    }

    private Mono<Void> postPayload(Map<String, Object> payload) {
        if (!configured) {
            log.info("Skipping WhatsApp API call because credentials are missing. PayloadSummary={}", redactPayload(payload));
            return Mono.empty();
        }
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment(phoneNumberId, "messages")
                        .build())
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .doOnNext(body -> log.debug("WhatsApp API response: {}", body))
                .doOnError(ex -> log.error("Failed to send WhatsApp message", ex))
                .then();
    }

    private Map<String, Object> redactPayload(Map<String, Object> payload) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("type", payload.get("type"));
        summary.put("to", maskPhoneNumber(Objects.toString(payload.get("to"), "")));

        String messageBody = extractMessageBody(payload);
        if (messageBody != null) {
            summary.put("messagePreview", truncateMessage(messageBody));
        }
        return Collections.unmodifiableMap(summary);
    }

    private String maskPhoneNumber(String to) {
        if (to == null || to.isBlank()) {
            return "";
        }
        String trimmed = to.trim();
        if (trimmed.length() <= 4) {
            return trimmed;
        }
        String lastFour = trimmed.substring(trimmed.length() - 4);
        return "*".repeat(trimmed.length() - 4) + lastFour;
    }

    @SuppressWarnings("unchecked")
    private String extractMessageBody(Map<String, Object> payload) {
        Object text = payload.get("text");
        if (text instanceof Map<?, ?> textMap) {
            Object body = textMap.get("body");
            if (body != null) {
                String value = body.toString();
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }

    private String truncateMessage(String message) {
        int maxLength = 100;
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }
}
