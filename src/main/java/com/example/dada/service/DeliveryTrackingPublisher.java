package com.example.dada.service;

import com.example.dada.dto.TripStatusNotification;
import com.example.dada.model.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryTrackingPublisher {

    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    /**
     * Publish the current status of a trip to WebSocket subscribers and cache the status in Redis when available.
     *
     * If a WebSocket messaging template is present, a TripStatusNotification for the given trip is sent to
     * the destination "/topic/trips/{tripId}". If a Redis template is present, the trip's status name is stored
     * in the Redis hash key "trip-status" under the trip ID and the key is set to expire after 24 hours.
     *
     * @param trip the trip whose status will be published and cached
     */
    public void publishStatusUpdate(Trip trip) {
        TripStatusNotification notification = new TripStatusNotification(
                trip.getId(),
                trip.getCustomer().getId(),
                trip.getRider() != null ? trip.getRider().getId() : null,
                trip.getStatus(),
                LocalDateTime.now()
        );

        Optional.ofNullable(messagingTemplateProvider.getIfAvailable())
                .ifPresent(template -> {
                    String destination = "/topic/trips/" + trip.getId();
                    template.convertAndSend(destination, notification);
                    log.debug("Published trip status to websocket destination {}", destination);
                });

        Optional.ofNullable(redisTemplateProvider.getIfAvailable())
                .ifPresent(redisTemplate -> {
                    String key = "trip-status";
                    redisTemplate.opsForHash().put(key, trip.getId().toString(), notification.status().name());
                    redisTemplate.expire(key, Duration.ofHours(24));
                    log.debug("Cached trip status for trip {} in Redis", trip.getId());
                });
    }
}
