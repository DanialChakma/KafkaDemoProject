package com.oms.saga.config;

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

        @Bean
        public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {

            DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                    template,
                    (record, exception) -> {

                        String dltTopic = record.topic() + ".DLT";

                        log.error(
                                "====== Kafka Message Failed ======\n" +
                                        "Original Topic: {}\n" +
                                        "Partition: {}\n" +
                                        "Offset: {}\n" +
                                        "Key: {}\n" +
                                        "Value: {}\n" +
                                        "Routing to DLT: {}\n" +
                                        "Exception:",
                                record.topic(),
                                record.partition(),
                                record.offset(),
                                record.key(),
                                record.value(),
                                dltTopic,
                                exception // <-- FULL STACKTRACE HERE
                        );

                        return new TopicPartition(dltTopic, record.partition());
                    }
            );

            FixedBackOff backOff = new FixedBackOff(1000L, 2);

            return new DefaultErrorHandler(recoverer, backOff);
        }

}




