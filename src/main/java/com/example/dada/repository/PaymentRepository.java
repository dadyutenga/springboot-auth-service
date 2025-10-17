package com.example.dada.repository;

import com.example.dada.enums.PaymentStatus;
import com.example.dada.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    /**
 * Finds the payment associated with the given trip identifier.
 *
 * @param tripId the UUID of the trip
 * @return an Optional containing the Payment for the specified tripId, or an empty Optional if none exists
 */
Optional<Payment> findByTripId(UUID tripId);
    /**
 * Finds a payment by its transaction identifier.
 *
 * @param transactionId the transaction identifier associated with the payment
 * @return an Optional containing the matching Payment if found, `Optional.empty()` otherwise
 */
Optional<Payment> findByTransactionId(String transactionId);
    /**
 * Finds payments with the specified status.
 *
 * @param status the payment status to filter by
 * @return a list of Payment entities that have the given status, or an empty list if none match
 */
List<Payment> findByStatus(PaymentStatus status);
}