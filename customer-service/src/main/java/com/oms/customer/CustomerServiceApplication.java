package com.oms.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.core.env.Environment;
//import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;

@SpringBootApplication
//@EnableEurekaClient
public class CustomerServiceApplication implements CommandLineRunner {

    @Autowired
    private Environment  env;

	public static void main(String[] args) {
//        System.setProperty("javax.net.debug", "ssl,handshake,trustmanager");
//
//        // Keystore: contains client cert & key for mTLS
//        System.setProperty("javax.net.ssl.keyStore", "src/main/resources/mTLS/authService-keystore.p12");
//        System.setProperty("javax.net.ssl.keyStorePassword", "Da123!@#");
//        System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");
//        System.setProperty("javax.net.ssl.keyAlias", "authservice");
//
//        // Truststore: contains Eureka server cert
//        System.setProperty("javax.net.ssl.trustStore", "src/main/resources/mTLS/authService-truststore.p12");
//        System.setProperty("javax.net.ssl.trustStorePassword", "Da123!@#");
//        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");



        SpringApplication.run(CustomerServiceApplication.class, args);
	}

    @Override
    public void run(String... args) {
//        System.out.println("server.ssl.key-store: " + env.getProperty("server.ssl.key-store"));
//        System.out.println("server.ssl.key-alias: " + env.getProperty("server.ssl.key-alias"));
//        System.out.println("server.ssl.trust-store: " + env.getProperty("server.ssl.trust-store"));
//        System.out.println();
//        System.out.println("javax.net.ssl.keyStore: " + env.getProperty("javax.net.ssl.keyStore"));
//        System.out.println("javax.net.ssl.keyAlias: " + env.getProperty("javax.net.ssl.keyAlias"));
//        System.out.println("javax.net.ssl.trustStore: " + env.getProperty("javax.net.ssl.trustStore"));

    }

}
