package com.example.dada.service;

import com.example.dada.dto.*;
import com.example.dada.enums.UserRole;
import com.example.dada.exception.InvalidOtpException;
import com.example.dada.exception.UserAlreadyExistsException;
import com.example.dada.exception.UserNotEnabledException;
import com.example.dada.model.User;
import com.example.dada.repository.UserRepository;
import com.example.dada.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${otp.expiration-minutes}")
    private int otpExpirationMinutes;

    @Value("${otp.length}")
    private int otpLength;

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        String otp = generateOtp();
        LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(otpExpirationMinutes);

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.CUSTOMER;

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(role)
                .password(passwordEncoder.encode(request.getPassword()))
                .otp(otp)
                .otpExpiry(otpExpiry)
                .enabled(false)
                .verified(false)
                .build();

        userRepository.save(user);
        log.info("User registered successfully: {}", request.getEmail());

        // Send OTP email asynchronously
        emailService.sendOtpEmail(request.getEmail(), otp);

        return new MessageResponse("Registration successful. Please check your email for OTP verification.");
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        log.info("Attempting to verify OTP for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getEnabled()) {
            throw new InvalidOtpException("Account already verified");
        }

        if (!user.isOtpValid()) {
            throw new InvalidOtpException("OTP has expired or is invalid");
        }

        if (!user.getOtp().equals(request.getOtp())) {
            throw new InvalidOtpException("Invalid OTP");
        }

        user.setEnabled(true);
        user.setVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        log.info("OTP verified successfully for email: {}", request.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .message("Email verified successfully")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Attempting to login user: {}", request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.getEnabled()) {
            throw new UserNotEnabledException("Please verify your email before logging in");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        log.info("User logged in successfully: {}", request.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .message("Login successful")
                .build();
    }

    @Transactional
    public MessageResponse resendOtp(String email) {
        log.info("Attempting to resend OTP for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getEnabled()) {
            throw new InvalidOtpException("Account already verified");
        }

        String otp = generateOtp();
        LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(otpExpirationMinutes);

        user.setOtp(otp);
        user.setOtpExpiry(otpExpiry);
        userRepository.save(user);

        emailService.sendOtpEmail(email, otp);
        log.info("OTP resent successfully to: {}", email);

        return new MessageResponse("OTP has been resent to your email");
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
}
