package com.oms.customer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class KafkaDebug implements CommandLineRunner {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Kafka bootstrap-servers = " + bootstrapServers);
    }
}

