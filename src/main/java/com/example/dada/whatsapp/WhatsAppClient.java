package com.example.dada.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

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
    public void sendTextMessage(String to, String message) {
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", message)
        );
        postPayload(payload);
    }

    private void postPayload(Map<String, Object> payload) {
        if (!configured) {
            log.info("Skipping WhatsApp API call because credentials are missing. Payload={}", payload);
            return;
        }
        try {
            webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment(phoneNumberId, "messages")
                            .build())
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(body -> log.debug("WhatsApp API response: {}", body))
                    .onErrorResume(ex -> {
                        log.error("Failed to send WhatsApp message", ex);
                        return Mono.empty();
                    })
                    .block();
        } catch (Exception ex) {
            log.error("Unexpected error when calling WhatsApp API", ex);
        }
    }
}
