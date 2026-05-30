package com.yuno.payment.orchestrator.payment;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yuno.payment.orchestrator.idempotency.DuplicateRequestIdException;
import com.yuno.payment.orchestrator.idempotency.IdempotencyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Payment API integration tests")
class PaymentApiIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IdempotencyService idempotencyService;

	// ── Happy paths ─────────────────────────────────────────────────────────

	@Test
	@DisplayName("TC-001: Creates CARD payment through Provider A")
	void createsCardPaymentThroughProviderA() throws Exception {
		mockMvc.perform(post("/api/v1/payments")
						.header("Request-Id", "card-success-001")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amount": 1200.50,
								  "currency": "USD",
								  "paymentMethod": "CARD",
								  "merchantReferenceId": "ORDER-1001"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("SUCCEEDED"))
				.andExpect(jsonPath("$.provider").value("A"))
				.andExpect(jsonPath("$.attempts", hasSize(1)));
	}

	@Test
	@DisplayName("TC-002: Creates UPI payment and fetches it by ID")
	void fetchesCreatedPaymentById() throws Exception {
		MvcResult createResult = mockMvc.perform(post("/api/v1/payments")
						.header("Request-Id", "fetch-payment-001")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amount": 50.00,
								  "currency": "USD",
								  "paymentMethod": "UPI",
								  "merchantReferenceId": "ORDER-1002"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		String paymentId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.paymentId");

		mockMvc.perform(get("/api/v1/payments/{paymentId}", paymentId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paymentId").value(paymentId))
				.andExpect(jsonPath("$.paymentMethod").value("UPI"))
				.andExpect(jsonPath("$.provider").value("B"));
	}

	// ── Idempotency ──────────────────────────────────────────────────────────

	@Test
	@DisplayName("TC-003: Rejects duplicate Request-Id with 409")
	void rejectsDuplicateRequestId() throws Exception {
		doThrow(new DuplicateRequestIdException("same requestId"))
				.when(idempotencyService).checkAndStore(any(), any());

		mockMvc.perform(post("/api/v1/payments")
						.header("Request-Id", "dup-request-001")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amount": 10.00,
								  "currency": "USD",
								  "paymentMethod": "CARD",
								  "merchantReferenceId": "ORDER-1004"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("same requestId"));
	}

	// ── Validation ───────────────────────────────────────────────────────────

	@Test
	@DisplayName("TC-004: Rejects invalid request with 400")
	void rejectsInvalidCreatePaymentRequest() throws Exception {
		mockMvc.perform(post("/api/v1/payments")
						.header("Request-Id", "invalid-request-001")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amount": 0,
								  "currency": "usd",
								  "paymentMethod": "CARD",
								  "merchantReferenceId": ""
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.violations.amount").exists())
				.andExpect(jsonPath("$.violations.currency").exists())
				.andExpect(jsonPath("$.violations.merchantReferenceId").exists());
	}

	// ── 404 ──────────────────────────────────────────────────────────────────

	@Test
	@DisplayName("TC-005: Returns 404 for unknown payment ID")
	void returnsNotFoundForUnknownPayment() throws Exception {
		mockMvc.perform(get("/api/v1/payments/{paymentId}", "missing-payment"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Payment not found: missing-payment"));
	}
}
