package com.oms.payment.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "")
public class KafkaTopicsConfig {

    private Map<String, String> topics;

    public Map<String, String> getTopics() {
        return topics;
    }

    public void setTopics(Map<String, String> topics) {
        this.topics = topics;
    }

    /**
     * Creates all topics listed in application.yml if they don't exist.
     */
    @Bean
    public List<NewTopic> createTopics() {
        List<NewTopic> newTopics = new ArrayList<>();

        topics.values().forEach(topicName -> {
            newTopics.add(
                    TopicBuilder.name(topicName)
                            .partitions(3)
                            .replicas(1)
                            .build()
            );
        });

        return newTopics;
    }
}


