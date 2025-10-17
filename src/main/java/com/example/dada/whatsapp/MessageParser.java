package com.example.dada.whatsapp;

import lombok.Getter;
import lombok.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility responsible for converting free-form WhatsApp messages into structured commands.
 */
@Component
public class MessageParser {

    private static final Pattern OTP_PATTERN = Pattern.compile("(?i)^otp\\s+(\\d{4,6})$");
    private static final Pattern RIDE_REQUEST_PATTERN = Pattern.compile("(?i)^(?:ride|deliver|delivery)\\s+(?:from)\\s+(.+)\\s+(?:to)\\s+(.+)$");
    private static final Pattern TRACK_PATTERN = Pattern.compile("(?i)^(?:track|status)\\s+([a-f0-9-]{8,})$");
    private static final Pattern REPORT_PATTERN = Pattern.compile("(?i)^report\\s+issue\\s+([a-f0-9-]{8,})\\s+(.+)$");
    private static final Pattern ACCEPT_PATTERN = Pattern.compile("(?i)^accept\\s+([a-f0-9-]{8,})$");
    private static final Pattern REJECT_PATTERN = Pattern.compile("(?i)^reject\\s+([a-f0-9-]{8,})$");
    private static final Pattern STATUS_PATTERN = Pattern.compile("(?i)^(picked up|in transit|delivered)\\s+([a-f0-9-]{8,})$");
    private static final Pattern RATE_PATTERN = Pattern.compile("(?i)^rate\\s+([a-f0-9-]{8,})\\s+(\\d)(?:\\s+(.+))?$");

    /**
     * Parse raw message text into a structured command with optional parameters.
     *
     * @param body the incoming WhatsApp message
     * @return the parsed command (never {@code null})
     */
    public ParsedCommand parse(String body) {
        if (body == null) {
            return ParsedCommand.unknown();
        }
        String text = body.trim();
        if (text.isBlank()) {
            return ParsedCommand.unknown();
        }

        if (text.equalsIgnoreCase("register")) {
            return new ParsedCommand(CommandType.REGISTER);
        }
        if (text.equalsIgnoreCase("help")) {
            return new ParsedCommand(CommandType.HELP);
        }
        if (text.equalsIgnoreCase("confirm")) {
            return new ParsedCommand(CommandType.CONFIRM);
        }
        if (text.equalsIgnoreCase("cancel")) {
            return new ParsedCommand(CommandType.CANCEL);
        }
        if (text.equalsIgnoreCase("earnings") || text.equalsIgnoreCase("summary")) {
            return new ParsedCommand(CommandType.EARNINGS_SUMMARY);
        }

        Matcher matcher = OTP_PATTERN.matcher(text);
        if (matcher.matches()) {
            return new ParsedCommand(CommandType.OTP, Map.of(ParamKey.OTP.name(), matcher.group(1)));
        }

        matcher = RIDE_REQUEST_PATTERN.matcher(text);
        if (matcher.matches()) {
            Map<String, String> params = new HashMap<>();
            params.put(ParamKey.PICKUP.name(), matcher.group(1).trim());
            params.put(ParamKey.DROPOFF.name(), matcher.group(2).trim());
            return new ParsedCommand(CommandType.RIDE_REQUEST, params);
        }

        matcher = TRACK_PATTERN.matcher(text);
        if (matcher.matches()) {
            return new ParsedCommand(CommandType.TRACK_TRIP, Map.of(ParamKey.TRIP_ID.name(), matcher.group(1)));
        }

        matcher = REPORT_PATTERN.matcher(text);
        if (matcher.matches()) {
            return new ParsedCommand(CommandType.REPORT_ISSUE, Map.of(
                    ParamKey.TRIP_ID.name(), matcher.group(1),
                    ParamKey.MESSAGE.name(), matcher.group(2).trim()
            ));
        }

        matcher = ACCEPT_PATTERN.matcher(text);
        if (matcher.matches()) {
            return new ParsedCommand(CommandType.ACCEPT_TRIP, Map.of(ParamKey.TRIP_ID.name(), matcher.group(1)));
        }

        matcher = REJECT_PATTERN.matcher(text);
        if (matcher.matches()) {
            return new ParsedCommand(CommandType.REJECT_TRIP, Map.of(ParamKey.TRIP_ID.name(), matcher.group(1)));
        }

        matcher = STATUS_PATTERN.matcher(text);
        if (matcher.matches()) {
            return new ParsedCommand(CommandType.UPDATE_TRIP_STATUS, Map.of(
                    ParamKey.STATUS.name(), matcher.group(1).toLowerCase(),
                    ParamKey.TRIP_ID.name(), matcher.group(2)
            ));
        }

        matcher = RATE_PATTERN.matcher(text);
        if (matcher.matches()) {
            Map<String, String> params = new HashMap<>();
            params.put(ParamKey.TRIP_ID.name(), matcher.group(1));
            params.put(ParamKey.RATING.name(), matcher.group(2));
            Optional.ofNullable(matcher.group(3)).ifPresent(comment ->
                    params.put(ParamKey.COMMENT.name(), comment.trim())
            );
            return new ParsedCommand(CommandType.RATE_TRIP, params);
        }

        return ParsedCommand.unknown();
    }

    /** Command identifiers supported by the WhatsApp integration. */
    @Getter
    public enum CommandType {
        REGISTER,
        OTP,
        RIDE_REQUEST,
        CONFIRM,
        CANCEL,
        TRACK_TRIP,
        REPORT_ISSUE,
        ACCEPT_TRIP,
        REJECT_TRIP,
        UPDATE_TRIP_STATUS,
        RATE_TRIP,
        EARNINGS_SUMMARY,
        HELP,
        UNKNOWN
    }

    /** Convenience keys used for command parameters. */
    public enum ParamKey {
        PICKUP,
        DROPOFF,
        TRIP_ID,
        OTP,
        MESSAGE,
        STATUS,
        RATING,
        COMMENT
    }

    @Value
    public static class ParsedCommand {
        CommandType type;
        Map<String, String> params;

        ParsedCommand(CommandType type) {
            this(type, Map.of());
        }

        public ParsedCommand(CommandType type, Map<String, String> params) {
            this.type = Objects.requireNonNull(type, "type must not be null");
            this.params = Map.copyOf(params);
        }

        public String getParam(String key) {
            return params.get(key);
        }

        public static ParsedCommand unknown() {
            return new ParsedCommand(CommandType.UNKNOWN, Map.of());
        }
    }
}
