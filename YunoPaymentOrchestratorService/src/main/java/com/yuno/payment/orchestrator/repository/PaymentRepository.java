package com.yuno.payment.orchestrator.repository;

import com.yuno.payment.orchestrator.repository.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {
}
