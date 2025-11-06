package com.oms.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentsCapturedTopic() {
        return new NewTopic("${topics.payments-success:payments.success}",
                3, (short) 1
        );
    }
}
