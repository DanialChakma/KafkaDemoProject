//package com.oms.config;
//
//import org.apache.kafka.clients.admin.NewTopic;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.config.TopicBuilder;
//
//@Configuration
//public class KafkaConfig {
//
//    @Value("${topics.order-created:orders.created}")
//    private String orderCreatedTopic;
//
//    @Value("${topics.inventory-reserved:inventory.reserved}")
//    private String inventoryReservedTopic;
//
//    // define topics if you want the application to create them
//    @Bean
//    public NewTopic orderCreatedTopic() {
//        return TopicBuilder.name(orderCreatedTopic).partitions(6).replicas(1).build();
//    }
//
//    @Bean
//    public NewTopic inventoryReservedTopic() {
//        return TopicBuilder.name(inventoryReservedTopic).partitions(6).replicas(1).build();
//    }
//
//    // ... create other topics similarly if desired
//}
//
