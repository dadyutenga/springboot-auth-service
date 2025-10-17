package com.example.dada.whatsapp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints exposed for WhatsApp webhook integration.
 */
@Slf4j
@RestController
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    @Value("${whatsapp.verify-token}")
    private String verifyToken;

    /**
     * WhatsApp verification callback.
     *
     * @param mode      verification mode
     * @param token     token provided by WhatsApp
     * @param challenge random challenge string to echo back when the token matches
     * @return the challenge when verification succeeds or 403 otherwise
     */
    @GetMapping("/verify-webhook")
    public ResponseEntity<String> verifyWebhook(@RequestParam(name = "hub.mode", required = false) String mode,
                                                @RequestParam(name = "hub.verify_token", required = false) String token,
                                                @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        log.warn("Invalid WhatsApp webhook verification attempt");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    /**
     * Primary webhook endpoint invoked by the WhatsApp Cloud API.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(@RequestBody WhatsAppWebhookRequest request) {
        whatsAppService.processWebhook(request);
        return ResponseEntity.ok().build();
    }
}
