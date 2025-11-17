package com.oms.customer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.customer.entity.Customer;
import com.oms.customer.entity.OutboxEvent;
import com.oms.customer.events.CustomerValidationResultEvent;
import com.oms.customer.events.OrderValidationRequestEvent;
import com.oms.customer.repository.CustomerRepository;
import com.oms.customer.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topics.customer-validation-result:customer.validation.result}")
    private String VALIDATION_RESULT_TOPIC;


    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public Optional<Customer> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    @Transactional
    public void handleOrderValidationRequest( OrderValidationRequestEvent event ){

        try{
            boolean exists = customerRepository.existsById(event.getCustomerId());
            CustomerValidationResultEvent result = new CustomerValidationResultEvent(
                    event.getOrderId(),
                    event.getCustomerId(),
                    exists,
                    event.getEventId()
            );

            this.publishEvent(result, VALIDATION_RESULT_TOPIC, result.getOrderId(), "customer-service");

        }catch (Exception e){
            log.error("Error during order validation: {}", e.getMessage());
        }
    }

    @Transactional
    public void publishEvent(Object event, String eventType, String aggregateId, String aggregateType) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            // 1️⃣ Persist Outbox first
            OutboxEvent outbox = OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventType(eventType) // eventType is the topic used for kafka
                    .published(false)
                    .payload(payload)
                    .createdAt(Instant.now())
                    .build();
            outboxRepository.save(outbox);

            // 2️⃣ Attempt to send immediately
            kafkaTemplate.send(eventType, aggregateId, payload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Kafka send success for {} [{}]", eventType, aggregateId);
                            markOutboxAsSent(outbox.getId());
                        } else {
                            log.error("Kafka send failed for {} [{}]: {}", eventType, aggregateId, ex.getMessage());
                            markOutboxAsFailed(outbox.getId());
                        }
                    });

        } catch (Exception e) {
            log.error("Error publishing event to Kafka/outbox: {}", e.getMessage());
        }
    }

    @Transactional
    protected void markOutboxAsFailed(Long outboxId) {
        outboxRepository.findById(outboxId).ifPresent(e -> {
            e.setRetryCount(e.getRetryCount() + 1);
            e.setLastAttemptAt(Instant.now());
            // exponential backoff: e.g., 2^retryCount seconds
            long delaySeconds = (long) Math.pow(2, e.getRetryCount());
            e.setNextAttemptAt(Instant.now().plusSeconds(delaySeconds));
            outboxRepository.save(e);
        });
    }

    @Transactional
    protected void markOutboxAsSent(Long outboxId) {
        outboxRepository.findById(outboxId).ifPresent(o -> {
            o.setPublished(true);
            o.setUpdatedAt(Instant.now());
            outboxRepository.save(o);
        });
    }

}



