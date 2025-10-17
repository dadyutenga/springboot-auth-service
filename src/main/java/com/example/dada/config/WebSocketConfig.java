package com.example.dada.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure the application's message broker and destination prefixes.
     *
     * Enables a simple in-memory message broker for destinations with the "/topic"
     * prefix and sets the application destination prefix to "/app" for messages
     * routed to message-handling methods.
     *
     * @param registry the MessageBrokerRegistry used to configure broker options
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registers the STOMP WebSocket endpoint used by clients to connect for tracking messages.
     *
     * The endpoint is exposed at "/ws/tracking", allows requests from any origin pattern,
     * and enables SockJS fallback for environments without native WebSocket support.
     *
     * @param registry the registry used to add and configure STOMP endpoints
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/tracking").setAllowedOriginPatterns("*").withSockJS();
    }
}
