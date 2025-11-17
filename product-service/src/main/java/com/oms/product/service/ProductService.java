package com.oms.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.product.entity.OutboxEvent;
import com.oms.product.entity.Product;
import com.oms.product.events.OrderItemDTO;
import com.oms.product.events.OrderValidationRequestEvent;
import com.oms.product.events.ProductValidationResultEvent;
import com.oms.product.repository.OutboxEventRepository;
import com.oms.product.repository.ProductRepository;
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
public class ProductService {

    private final ProductRepository productRepository;
    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${product-validation-result:product.validation.result}")
    private String VALIDATION_RESULT_TOPIC;


    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Optional<Product> getProductBySku(String skuCode) {
        return productRepository.findBySkuCode(skuCode);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void handleOrderValidationRequest(OrderValidationRequestEvent event){
        try{

            boolean allExist = event.getItems().stream().map(OrderItemDTO::getProductId)
                    .allMatch(productRepository::existsById);

            ProductValidationResultEvent result = ProductValidationResultEvent.builder()
                    .eventId(event.getEventId())
                    .valid(allExist)
                    .orderId(event.getOrderId())
                    .customerId(event.getCustomerId())
                    .items(event.getItems())
                    .build();

            this.publishEvent(result, VALIDATION_RESULT_TOPIC, result.getOrderId(), "product-service");

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


