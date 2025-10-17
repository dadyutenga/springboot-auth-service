package com.example.dada.whatsapp;

import com.example.dada.dto.MessageResponse;
import com.example.dada.dto.RatingRequestDto;
import com.example.dada.dto.RegisterRequest;
import com.example.dada.dto.ReportRequestDto;
import com.example.dada.dto.TripRequestDto;
import com.example.dada.dto.TripStatusUpdateDto;
import com.example.dada.dto.VerifyOtpRequest;
import com.example.dada.enums.TripStatus;
import com.example.dada.enums.UserRole;
import com.example.dada.exception.BadRequestException;
import com.example.dada.exception.ResourceNotFoundException;
import com.example.dada.exception.UserAlreadyExistsException;
import com.example.dada.model.Trip;
import com.example.dada.model.User;
import com.example.dada.repository.UserRepository;
import com.example.dada.service.AuthService;
import com.example.dada.service.RatingService;
import com.example.dada.service.ReportService;
import com.example.dada.service.RiderService;
import com.example.dada.service.TripService;
import com.example.dada.whatsapp.MessageParser.CommandType;
import com.example.dada.whatsapp.MessageParser.ParamKey;
import com.example.dada.whatsapp.MessageParser.ParsedCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import reactor.core.publisher.Mono;

/**
 * Orchestrates WhatsApp conversations and bridges them to domain services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private static final BigDecimal BASE_FARE = BigDecimal.valueOf(150);
    private static final BigDecimal PER_KM_RATE = BigDecimal.valueOf(65);
    private static final BigDecimal DEFAULT_DISTANCE_KM = BigDecimal.valueOf(5);
    private static final char[] BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MIN_TEMP_TOKEN_LENGTH = 12;
    private static final int MAX_TEMP_TOKEN_LENGTH = 16;
    private static final Duration TEMP_TOKEN_TTL = Duration.ofMinutes(10);

    private final MessageParser messageParser;
    private final WhatsAppClient whatsAppClient;
    private final AuthService authService;
    private final TripService tripService;
    private final ReportService reportService;
    private final RiderService riderService;
    private final RatingService ratingService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final Map<String, ConversationState> conversations = new ConcurrentHashMap<>();

    @Value("${whatsapp.admin-notify-number:}")
    private String adminNotifyNumber;

    /**
     * Handle a webhook payload sent by the WhatsApp Cloud API.
     *
     * @param request payload containing incoming messages
     */
    public void processWebhook(WhatsAppWebhookRequest request) {
        if (request == null || request.getEntry() == null) {
            return;
        }
        request.getEntry().stream()
                .filter(entry -> entry.getChanges() != null)
                .flatMap(entry -> entry.getChanges().stream())
                .map(WhatsAppWebhookRequest.Change::getValue)
                .filter(value -> value.getMessages() != null)
                .flatMap(value -> value.getMessages().stream())
                .forEach(message -> handleIncomingMessage(message.getFrom(), message.body()));
    }

    private void handleIncomingMessage(String rawFrom, String body) {
        if (rawFrom == null) {
            log.warn("Received WhatsApp message without sender information");
            return;
        }
        String phone = normalizePhoneNumber(rawFrom);
        ConversationState state = conversations.computeIfAbsent(phone, key -> ConversationState.idle());

        if (body == null || body.isBlank()) {
            sendMessage(phone, "Sorry, I didn't catch that. Type 'help' for available commands.");
            return;
        }

        if (handleConversationState(phone, body, state)) {
            return;
        }

        ParsedCommand command = messageParser.parse(body);
        switch (command.getType()) {
            case REGISTER -> startRegistration(phone, state);
            case OTP -> handleOtp(phone, state, command);
            case RIDE_REQUEST -> handleRideRequest(phone, state, command);
            case CONFIRM -> handleRideConfirmation(phone, state);
            case CANCEL -> handleCancellation(phone, state);
            case TRACK_TRIP -> handleTripTracking(phone, command);
            case REPORT_ISSUE -> handleReportIssue(phone, command);
            case ACCEPT_TRIP -> handleRiderAcceptance(phone, command);
            case REJECT_TRIP -> handleRiderRejection(phone, command);
            case UPDATE_TRIP_STATUS -> handleStatusUpdate(phone, command);
            case RATE_TRIP -> handleRating(phone, command);
            case EARNINGS_SUMMARY -> handleEarningsSummary(phone);
            case HELP -> sendHelpMessage(phone);
            default -> sendMessage(phone, "I'm not sure how to help with that. Type 'help' for guidance.");
        }
    }

    private void sendMessage(String phone, String message) {
        whatsAppClient.sendTextMessage(phone, message)
                .doOnError(ex -> log.error("Failed to deliver WhatsApp message to {}", phone, ex))
                .onErrorResume(ex -> Mono.empty())
                .subscribe();
    }

    private boolean handleConversationState(String phone, String body, ConversationState state) {
        return switch (state.getStage()) {
            case AWAITING_EMAIL -> {
                if (!body.contains("@")) {
                    sendMessage(phone, "Please share a valid email address to continue registration.");
                    yield true;
                }
                state.setPendingEmail(body.trim());
                state.setStage(ConversationState.Stage.AWAITING_NAME);
                sendMessage(phone, "Great! What's your full name?");
                yield true;
            }
            case AWAITING_NAME -> {
                state.setPendingFullName(body.trim());
                state.setStage(ConversationState.Stage.AWAITING_ROLE);
                sendMessage(phone, "Are you registering as a customer or rider?");
                yield true;
            }
            case AWAITING_ROLE -> {
                handleRoleSelection(phone, state, body);
                yield true;
            }
            case AWAITING_OTP -> {
                ParsedCommand command = messageParser.parse(body);
                if (command.getType() == CommandType.OTP) {
                    handleOtp(phone, state, command);
                } else {
                    sendMessage(phone, "Please send your OTP using 'otp 123456'.");
                }
                yield true;
            }
            case AWAITING_RIDE_CONFIRMATION -> {
                ParsedCommand command = messageParser.parse(body);
                if (command.getType() == CommandType.CONFIRM) {
                    handleRideConfirmation(phone, state);
                } else if (command.getType() == CommandType.CANCEL) {
                    handleCancellation(phone, state);
                } else {
                    sendMessage(phone, "Reply with 'confirm' to book or 'cancel' to abort.");
                }
                yield true;
            }
            default -> false;
        };
    }

    private void startRegistration(String phone, ConversationState state) {
        Optional<User> existing = userRepository.findByPhone(phone);
        if (existing.isPresent()) {
            sendMessage(phone, "You're already registered. You can request a ride anytime!");
            state.reset();
            return;
        }
        state.reset();
        state.setStage(ConversationState.Stage.AWAITING_EMAIL);
        sendMessage(phone, "Welcome to DaDa Express! Please share your email address to begin registration.");
    }

    private void handleRoleSelection(String phone, ConversationState state, String body) {
        String normalized = body.trim().toLowerCase();
        if (normalized.contains("rider")) {
            state.setPendingRole(UserRole.RIDER);
        } else if (normalized.contains("customer") || normalized.contains("deliver")) {
            state.setPendingRole(UserRole.CUSTOMER);
        } else {
            sendMessage(phone, "Please reply with either 'customer' or 'rider'.");
            return;
        }

        GeneratedCredential credential = generateTemporaryCredential();
        state.setTemporaryCredential(credential.storedCredential());
        String tempPassword = credential.rawToken();

        RegisterRequest request = new RegisterRequest(
                state.getPendingFullName(),
                state.getPendingEmail(),
                phone,
                state.getPendingRole(),
                tempPassword
        );

        try {
            MessageResponse response = authService.register(request);
            sendMessage(phone, response.getMessage() + " Use password: " + tempPassword + " to access the app.");
            sendMessage(phone, "Please enter the OTP sent to " + state.getPendingEmail() + " using 'otp 123456'.");
            state.setStage(ConversationState.Stage.AWAITING_OTP);
        } catch (UserAlreadyExistsException ex) {
            sendMessage(phone, "This email is already registered. Try logging in via the app.");
            state.reset();
        } catch (Exception ex) {
            log.error("Failed to register WhatsApp user", ex);
            sendMessage(phone, "We couldn't complete your registration. Please try again later.");
            state.reset();
        }
    }

    private void handleOtp(String phone, ConversationState state, ParsedCommand command) {
        if (state.getPendingEmail() == null) {
            sendMessage(phone, "Start registration by sending 'register'.");
            return;
        }
        ConversationState.TemporaryCredential credential = state.getTemporaryCredential();
        if (credential != null && credential.isUsed()) {
            sendMessage(phone, "Your temporary access code has already been used. Please restart registration.");
            state.reset();
            return;
        }

        if (credential != null && credential.isExpired()) {
            sendMessage(phone, "Your temporary access code has expired. Please restart registration.");
            state.reset();
            return;
        }

        String otp = command.getParam(ParamKey.OTP.name());
        try {
            authService.verifyOtp(new VerifyOtpRequest(state.getPendingEmail(), otp));
            sendMessage(phone, "Registration complete! You can now request rides via WhatsApp.");
        } catch (Exception ex) {
            sendMessage(phone, "We couldn't verify that OTP. Please ensure it's correct and try again.");
            return;
        }
        if (credential != null) {
            credential.setUsed(true);
        }
        state.reset();
    }

    private void handleRideRequest(String phone, ConversationState state, ParsedCommand command) {
        Optional<User> userOptional = userRepository.findByPhone(phone);
        if (userOptional.isEmpty()) {
            sendMessage(phone, "Please register first by sending 'register'.");
            return;
        }
        User user = userOptional.get();
        if (user.getRole() != UserRole.CUSTOMER) {
            sendMessage(phone, "Only customers can request deliveries. Riders can type 'earnings' to view their summary.");
            return;
        }

        String pickupRaw = command.getParam(ParamKey.PICKUP.name());
        String dropoffRaw = command.getParam(ParamKey.DROPOFF.name());
        String pickup = pickupRaw == null ? null : pickupRaw.trim();
        String dropoff = dropoffRaw == null ? null : dropoffRaw.trim();
        if (pickup == null || pickup.isEmpty() || dropoff == null || dropoff.isEmpty()) {
            sendMessage(phone, "Please provide both pickup and dropoff locations using 'ride from <pickup> to <dropoff>'.");
            return;
        }

        BigDecimal distance = estimateDistance(pickup, dropoff);
        BigDecimal fare = estimateFare(distance);

        state.reset();
        state.setPendingPickup(pickup);
        state.setPendingDropoff(dropoff);
        state.setPendingDistance(distance);
        state.setPendingFare(fare);
        state.setStage(ConversationState.Stage.AWAITING_RIDE_CONFIRMATION);

        sendMessage(phone, String.format(
                "Trip estimate from %s to %s is KES %s (~%s km). Reply 'confirm' to book or 'cancel' to abort.",
                pickup,
                dropoff,
                fare.setScale(0, RoundingMode.HALF_UP),
                distance.setScale(1, RoundingMode.HALF_UP)
        ));
    }

    private void handleRideConfirmation(String phone, ConversationState state) {
        if (state.getStage() != ConversationState.Stage.AWAITING_RIDE_CONFIRMATION) {
            sendMessage(phone, "No trip pending confirmation. Send 'ride from <pickup> to <dropoff>'.");
            return;
        }
        Optional<User> userOptional = userRepository.findByPhone(phone);
        if (userOptional.isEmpty()) {
            sendMessage(phone, "Please register first by sending 'register'.");
            state.reset();
            return;
        }

        TripRequestDto request = new TripRequestDto();
        request.setPickupLocation(state.getPendingPickup());
        request.setDropoffLocation(state.getPendingDropoff());
        request.setDistanceKm(Optional.ofNullable(state.getPendingDistance()).orElse(DEFAULT_DISTANCE_KM));
        request.setFare(Optional.ofNullable(state.getPendingFare()).orElse(estimateFare(DEFAULT_DISTANCE_KM)));

        try {
            var response = tripService.createTripForUser(userOptional.get(), request);
            state.reset();
            sendMessage(phone, "Trip created! We'll notify you when a rider accepts. Trip ID: " + response.getId());
        } catch (BadRequestException | ResourceNotFoundException ex) {
            sendMessage(phone, ex.getMessage());
            state.reset();
        } catch (Exception ex) {
            log.error("Failed to create trip from WhatsApp", ex);
            sendMessage(phone, "We couldn't create your trip right now. Please try again later.");
            state.reset();
        }
    }

    private void handleCancellation(String phone, ConversationState state) {
        if (state.getStage() == ConversationState.Stage.AWAITING_RIDE_CONFIRMATION) {
            sendMessage(phone, "No worries, your trip request has been cancelled.");
            state.reset();
        } else {
            sendMessage(phone, "There is no pending action to cancel right now.");
        }
    }

    private void handleTripTracking(String phone, ParsedCommand command) {
        Optional<User> optionalUser = userRepository.findByPhone(phone);
        if (optionalUser.isEmpty()) {
            sendMessage(phone, "Please register first by sending 'register'.");
            return;
        }
        UUID tripId = parseUuid(command.getParam(ParamKey.TRIP_ID.name()));
        if (tripId == null) {
            sendMessage(phone, "Please provide a valid trip ID, e.g. 'track 123e4567-e89b-12d3-a456-426614174000'.");
            return;
        }
        try {
            var trip = tripService.getTripDetailsForUser(tripId, optionalUser.get());
            sendMessage(phone, String.format(
                    "Trip %s is currently %s.", trip.getId(), trip.getStatus()));
        } catch (Exception ex) {
            sendMessage(phone, "We couldn't find that trip or you don't have access to it.");
        }
    }

    private void handleReportIssue(String phone, ParsedCommand command) {
        Optional<User> optionalUser = userRepository.findByPhone(phone);
        if (optionalUser.isEmpty()) {
            sendMessage(phone, "Please register first by sending 'register'.");
            return;
        }
        UUID tripId = parseUuid(command.getParam(ParamKey.TRIP_ID.name()));
        if (tripId == null) {
            sendMessage(phone, "Please include a valid trip ID when reporting an issue.");
            return;
        }
        ReportRequestDto request = new ReportRequestDto();
        request.setTripId(tripId);
        request.setReason("WHATSAPP_REPORT");
        request.setDescription(command.getParam(ParamKey.MESSAGE.name()));
        try {
            var response = reportService.createReportForUser(optionalUser.get(), request);
            sendMessage(phone, "Thanks! We've logged your report and our support team will review it.");
            notifyAdminOfReport(response.getId(), response.getTripId(), command.getParam(ParamKey.MESSAGE.name()));
        } catch (Exception ex) {
            sendMessage(phone, "We couldn't create your report. Ensure the trip belongs to you and has been completed.");
        }
    }

    private void handleRiderAcceptance(String phone, ParsedCommand command) {
        Optional<User> optionalUser = userRepository.findByPhone(phone);
        if (optionalUser.isEmpty()) {
            sendMessage(phone, "Please register as a rider first by sending 'register'.");
            return;
        }
        User rider = optionalUser.get();
        UUID tripId = parseUuid(command.getParam(ParamKey.TRIP_ID.name()));
        if (tripId == null) {
            sendMessage(phone, "Include the trip id, e.g. 'accept 123e4567-e89b-12d3-a456-426614174000'.");
            return;
        }
        try {
            tripService.acceptTripForUser(tripId, rider);
            sendMessage(phone, "Trip accepted! Remember to update status as you progress.");
        } catch (BadRequestException ex) {
            sendMessage(phone, ex.getMessage());
        } catch (ResourceNotFoundException ex) {
            sendMessage(phone, "We couldn't find that trip or you don't have permission to accept it.");
        } catch (Exception ex) {
            log.error("Failed to accept trip {} via WhatsApp", tripId, ex);
            sendMessage(phone, "Unable to accept that trip right now. Please try again later.");
        }
    }

    private void handleRiderRejection(String phone, ParsedCommand command) {
        UUID tripId = parseUuid(command.getParam(ParamKey.TRIP_ID.name()));
        if (tripId == null) {
            sendMessage(phone, "Include the trip id when rejecting, e.g. 'reject <tripId>'.");
            return;
        }
        sendMessage(phone, "Trip " + tripId + " has been marked as declined. We'll reassign it to another rider.");
    }

    private void handleStatusUpdate(String phone, ParsedCommand command) {
        Optional<User> optionalUser = userRepository.findByPhone(phone);
        if (optionalUser.isEmpty()) {
            sendMessage(phone, "Please register first by sending 'register'.");
            return;
        }
        UUID tripId = parseUuid(command.getParam(ParamKey.TRIP_ID.name()));
        if (tripId == null) {
            sendMessage(phone, "Please include a valid trip id when updating status.");
            return;
        }
        TripStatus status = mapStatus(command.getParam(ParamKey.STATUS.name()));
        if (status == null) {
            sendMessage(phone, "Unknown status. Use 'picked up', 'in transit', or 'delivered'.");
            return;
        }
        TripStatusUpdateDto updateDto = new TripStatusUpdateDto();
        updateDto.setStatus(status);
        try {
            tripService.updateTripStatusForUser(tripId, optionalUser.get(), updateDto);
            sendMessage(phone, "Status updated to " + status + ".");
        } catch (BadRequestException ex) {
            sendMessage(phone, ex.getMessage());
        } catch (ResourceNotFoundException ex) {
            sendMessage(phone, "We couldn't find that trip or you don't have access to update it.");
        } catch (Exception ex) {
            log.error("Failed to update trip {} status via WhatsApp", tripId, ex);
            sendMessage(phone, "Unable to update status at this time.");
        }
    }

    private void handleRating(String phone, ParsedCommand command) {
        Optional<User> optionalUser = userRepository.findByPhone(phone);
        if (optionalUser.isEmpty()) {
            sendMessage(phone, "Please register first by sending 'register'.");
            return;
        }
        UUID tripId = parseUuid(command.getParam(ParamKey.TRIP_ID.name()));
        if (tripId == null) {
            sendMessage(phone, "Please include a valid trip id when rating.");
            return;
        }
        Integer ratingValue;
        try {
            ratingValue = Integer.parseInt(command.getParam(ParamKey.RATING.name()));
            ratingValue = Math.max(1, Math.min(5, ratingValue));
        } catch (NumberFormatException ex) {
            sendMessage(phone, "Ratings must be a number between 1 and 5.");
            return;
        }
        try {
            Trip trip = tripService.getTripForParticipant(tripId, optionalUser.get());
            UUID targetUserId;
            if (optionalUser.get().getRole() == UserRole.CUSTOMER) {
                if (trip.getRider() == null) {
                    sendMessage(phone, "This trip has no rider assigned yet.");
                    return;
                }
                targetUserId = trip.getRider().getId();
            } else {
                targetUserId = trip.getCustomer().getId();
            }
            RatingRequestDto ratingRequest = new RatingRequestDto();
            ratingRequest.setTripId(tripId);
            ratingRequest.setTargetUserId(targetUserId);
            ratingRequest.setRatingValue(ratingValue);
            ratingRequest.setComment(command.getParam(ParamKey.COMMENT.name()));
            ratingService.submitRatingForUser(optionalUser.get(), ratingRequest);
            sendMessage(phone, "Thank you for your feedback!");
        } catch (BadRequestException ex) {
            sendMessage(phone, ex.getMessage());
        } catch (ResourceNotFoundException ex) {
            sendMessage(phone, "We couldn't find that trip or you don't have access to rate it.");
        } catch (Exception ex) {
            log.error("Failed to submit rating via WhatsApp for trip {}", tripId, ex);
            sendMessage(phone, "Unable to submit rating right now. Please try again later.");
        }
    }

    private void handleEarningsSummary(String phone) {
        Optional<User> optionalUser = userRepository.findByPhone(phone);
        if (optionalUser.isEmpty()) {
            sendMessage(phone, "Please register first by sending 'register'.");
            return;
        }
        User user = optionalUser.get();
        if (user.getRole() != UserRole.RIDER) {
            sendMessage(phone, "Earnings summary is available for riders only.");
            return;
        }
        try {
            var riderProfile = riderService.getRiderProfileForUser(user);
            sendMessage(phone, String.format(
                    "Earnings summary: %s earnings. Current rating: %s.",
                    riderProfile.getTotalEarnings() == null ? "KES 0" : "KES " + riderProfile.getTotalEarnings(),
                    riderProfile.getRating() == null ? "N/A" : riderProfile.getRating()
            ));
        } catch (Exception ex) {
            log.error("Failed to retrieve rider profile for user {} via WhatsApp", user.getId(), ex);
            sendMessage(phone, "Unable to retrieve your rider profile right now. Please try again later.");
        }
    }

    private void sendHelpMessage(String phone) {
        sendMessage(phone, "Commands: 'register', 'ride from <pickup> to <dropoff>', 'track <tripId>', 'report issue <tripId> <message>', 'rate <tripId> <1-5> <comment>'. Riders can use 'accept <tripId>' or 'picked up <tripId>'.");
    }

    private void notifyAdminOfReport(UUID reportId, UUID tripId, String message) {
        if (adminNotifyNumber == null || adminNotifyNumber.isBlank()) {
            return;
        }
        String body = String.format("New report %s for trip %s: %s", reportId, tripId, message);
        sendMessage(adminNotifyNumber, body);
    }

    private BigDecimal estimateDistance(String pickup, String dropoff) {
        int heuristic = Math.max(3, Math.min(20, Math.abs(pickup.length() - dropoff.length()) + 5));
        return BigDecimal.valueOf(heuristic);
    }

    private BigDecimal estimateFare(BigDecimal distanceKm) {
        return BASE_FARE.add(distanceKm.multiply(PER_KM_RATE));
    }

    private TripStatus mapStatus(String statusText) {
        if (statusText == null) {
            return null;
        }
        String normalized = statusText.trim().toLowerCase();
        return switch (normalized) {
            case "picked up" -> TripStatus.PICKED_UP;
            case "in transit" -> TripStatus.IN_TRANSIT;
            case "delivered" -> TripStatus.DELIVERED;
            default -> null;
        };
    }

    private UUID parseUuid(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizePhoneNumber(String rawFrom) {
        String trimmed = rawFrom.trim();
        if (trimmed.startsWith("+")) {
            return trimmed;
        }
        return "+" + trimmed;
    }

    private GeneratedCredential generateTemporaryCredential() {
        int lengthRange = MAX_TEMP_TOKEN_LENGTH - MIN_TEMP_TOKEN_LENGTH + 1;
        int length = MIN_TEMP_TOKEN_LENGTH + SECURE_RANDOM.nextInt(lengthRange);
        StringBuilder tokenBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            tokenBuilder.append(BASE62_ALPHABET[SECURE_RANDOM.nextInt(BASE62_ALPHABET.length)]);
        }
        String rawToken = tokenBuilder.toString();
        ConversationState.TemporaryCredential storedCredential = new ConversationState.TemporaryCredential(
                passwordEncoder.encode(rawToken),
                Instant.now().plus(TEMP_TOKEN_TTL),
                false
        );
        return new GeneratedCredential(rawToken, storedCredential);
    }

    private record GeneratedCredential(String rawToken, ConversationState.TemporaryCredential storedCredential) {
    }
}
