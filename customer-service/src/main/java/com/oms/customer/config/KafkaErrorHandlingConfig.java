package com.oms.customer.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaErrorHandlingConfig {

//    @Bean
//    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
//
//        // Publishes failed messages to topic-name.DLT
//        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
//                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition())
//        );
//
//        // how many retries before going to DLT
//        FixedBackOff backOff = new FixedBackOff(1000L, 2); // retry 2 times
//
//        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
//
//        // optional: ignore certain exceptions
//        // handler.addNotRetryableExceptions(YourCustomException.class);
//
//        return handler;
//    }


    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template, (record, exception) -> {

            String dltTopic = record.topic() + ".DLT";

            log.error(
                    "\n====== Kafka Message Failed ======" +
                            "\nOriginal Topic: {}" +
                            "\nPartition: {}" +
                            "\nOffset: {}" +
                            "\nKey: {}" +
                            "\nValue: {}" +
                            "\nException: {}" +
                            "\nRouting to DLT: {}" +
                            "\n====================================",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    record.value(),
                    exception.getMessage(),
                    dltTopic
            );

            // Route to topic-name.DLT
            return new TopicPartition(dltTopic, record.partition());
        });

        FixedBackOff backOff = new FixedBackOff(1000L, 2);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        return handler;
    }

}

