package com.yuno.payment.orchestrator.repository;

import java.util.List;

import com.yuno.payment.orchestrator.repository.entity.PaymentAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttemptEntity, Long> {

	List<PaymentAttemptEntity> findByPaymentIdOrderByAttemptNumberAsc(String transactionId);
}
