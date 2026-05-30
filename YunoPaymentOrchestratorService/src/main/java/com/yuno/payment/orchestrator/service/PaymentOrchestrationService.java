package com.yuno.payment.orchestrator.service;

import com.yuno.payment.orchestrator.api.model.CreatePaymentRequest;
import com.yuno.payment.orchestrator.api.model.PaymentResponse;
import com.yuno.payment.orchestrator.domain.PaymentMethod;
import com.yuno.payment.orchestrator.domain.PaymentStatus;
import com.yuno.payment.orchestrator.domain.ProviderCode;
import com.yuno.payment.orchestrator.exception.PaymentNotFoundException;
import com.yuno.payment.orchestrator.idempotency.IdempotencyService;
import com.yuno.payment.orchestrator.repository.entity.PaymentAttemptEntity;
import com.yuno.payment.orchestrator.repository.PaymentAttemptRepository;
import com.yuno.payment.orchestrator.repository.entity.PaymentEntity;
import com.yuno.payment.orchestrator.repository.PaymentRepository;
import com.yuno.payment.orchestrator.model.PaymentProviderRequest;
import com.yuno.payment.orchestrator.model.PaymentProviderResponse;
import com.yuno.payment.orchestrator.provider.ProviderConnector;
import com.yuno.payment.orchestrator.routing.RoutingDecision;
import com.yuno.payment.orchestrator.routing.RoutingEngine;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class PaymentOrchestrationService {

	private static final int PRIMARY_PROVIDER_MAX_ATTEMPTS = 2;

	private final PaymentRepository paymentRepository;
	private final PaymentAttemptRepository attemptRepository;
	private final IdempotencyService idempotencyService;
	private final RoutingEngine routingEngine;
	private final PaymentMapper paymentMapper;
	private final Map<ProviderCode, ProviderConnector> connectors;

	public PaymentOrchestrationService(
			PaymentRepository paymentRepository,
			PaymentAttemptRepository attemptRepository,
			IdempotencyService idempotencyService,
			RoutingEngine routingEngine,
			PaymentMapper paymentMapper,
			List<ProviderConnector> providerConnectors) {
		this.paymentRepository = paymentRepository;
		this.attemptRepository = attemptRepository;
		this.idempotencyService = idempotencyService;
		this.routingEngine = routingEngine;
		this.paymentMapper = paymentMapper;
		this.connectors = new EnumMap<>(ProviderCode.class);
		providerConnectors.forEach(c -> this.connectors.put(c.providerCode(), c));
	}

	@Transactional
	public PaymentResponse createPayment(String requestId, CreatePaymentRequest request) {
		// Build entity first — UUID is generated inside the constructor
		PaymentEntity payment = new PaymentEntity(
				request.getAmount(),
				request.getCurrency(),
				toDomain(request.getPaymentMethod()),
				request.getMerchantReferenceId()
		);

		// Atomically claim the requestId in Redis; throws DuplicateRequestIdException if already used
		idempotencyService.checkAndStore(requestId, payment.getId());

		// Persist transaction and run the provider orchestration loop
		paymentRepository.save(payment);
		processPayment(payment, request);

		return paymentMapper.toResponse(payment, attemptsFor(payment));
	}

	@Transactional(readOnly = true)
	public PaymentResponse getPayment(String paymentId) {
		PaymentEntity payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new PaymentNotFoundException(paymentId));
		return paymentMapper.toResponse(payment, attemptsFor(payment));
	}

	private void processPayment(PaymentEntity payment, CreatePaymentRequest request) {
		payment.setStatus(PaymentStatus.PROCESSING);

		RoutingDecision decision = routingEngine.route(payment.getPaymentMethod());
		PaymentProviderRequest providerRequest = new PaymentProviderRequest(
				payment.getId(),
				payment.getAmount(),
				payment.getCurrency(),
				payment.getPaymentMethod(),
				payment.getMerchantReferenceId()
		);

		// Primary provider — up to PRIMARY_PROVIDER_MAX_ATTEMPTS retries
		int attemptNumber = 1;
		for (int retry = 0; retry < PRIMARY_PROVIDER_MAX_ATTEMPTS; retry++) {
			PaymentProviderResponse response = authorize(decision.primaryProvider(), providerRequest);
			recordAttempt(payment, decision.primaryProvider(), attemptNumber++, response);
			if (response.successful()) {
				markSucceeded(payment, decision.primaryProvider(), response.providerReference());
				return;
			}
		}

		// Failover provider — one attempt
		PaymentProviderResponse failover = authorize(decision.failoverProvider(), providerRequest);
		recordAttempt(payment, decision.failoverProvider(), attemptNumber, failover);
		if (failover.successful()) {
			markSucceeded(payment, decision.failoverProvider(), failover.providerReference());
			return;
		}

		payment.setStatus(PaymentStatus.FAILED);
		payment.setFailureReason(failover.failureReason());
	}

	private PaymentProviderResponse authorize(ProviderCode providerCode, PaymentProviderRequest request) {
		ProviderConnector connector = connectors.get(providerCode);
		if (connector == null) {
			return PaymentProviderResponse.failure("No connector registered for provider " + providerCode);
		}
		return connector.authorize(request);
	}

	private void recordAttempt(PaymentEntity payment, ProviderCode provider, int attemptNumber,
			PaymentProviderResponse response) {
		PaymentStatus status = response.successful() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
		attemptRepository.save(new PaymentAttemptEntity(
				payment, provider, attemptNumber, status,
				response.providerReference(), response.failureReason()
		));
	}

	private void markSucceeded(PaymentEntity payment, ProviderCode provider, String providerReference) {
		payment.setStatus(PaymentStatus.SUCCEEDED);
		payment.setProvider(provider);
		payment.setProviderPaymentId(providerReference);
		payment.setFailureReason(null);
	}

	private List<PaymentAttemptEntity> attemptsFor(PaymentEntity payment) {
		return attemptRepository.findByPaymentIdOrderByAttemptNumberAsc(payment.getId());
	}

	private PaymentMethod toDomain(com.yuno.payment.orchestrator.api.model.PaymentMethod paymentMethod) {
		return PaymentMethod.valueOf(paymentMethod.name());
	}
}
