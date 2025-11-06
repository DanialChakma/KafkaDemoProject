//package com.ims.config;
//
//import org.apache.http.conn.ssl.NoopHostnameVerifier;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
//import org.apache.http.ssl.SSLContexts;
//import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
//import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
//
//import javax.net.ssl.KeyManagerFactory;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.TrustManagerFactory;
//import java.io.FileInputStream;
//import java.security.KeyStore;
//
//@Configuration
//public class EurekaClientSSLConfig {
//
//    private static final String KEYSTORE_PATH = "src/main/resources/mTLS/authService-keystore.p12";
//    private static final String KEY_ALIAS = "authservice";
//    private static final String TRUSTSTORE_PATH = "src/main/resources/mTLS/authService-truststore.p12";
//    private static final String PASSWORD = "Da123!@#";
//
//    @Bean
//    public RestTemplateDiscoveryClientOptionalArgs discoveryClientOptionalArgs() throws Exception {
//
//        // Load KeyStore (contains client cert)
//        KeyStore keyStore = KeyStore.getInstance("PKCS12");
//        keyStore.load(new FileInputStream(KEYSTORE_PATH), PASSWORD.toCharArray());
//
//        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//        kmf.init(keyStore, PASSWORD.toCharArray());
//
//        // Load TrustStore (contains Eureka server CA)
//        KeyStore trustStore = KeyStore.getInstance("PKCS12");
//        trustStore.load(new FileInputStream(TRUSTSTORE_PATH), PASSWORD.toCharArray());
//
//        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//        tmf.init(trustStore);
//
//        // Initialize SSLContext with key managers and trust managers
//        SSLContext sslContext = SSLContexts.custom()
//                .setProtocol("TLS")
//                .loadKeyMaterial(keyStore, PASSWORD.toCharArray(), (aliases, socket) -> KEY_ALIAS) // client certificate
////                .loadKeyMaterial(keyStore, PASSWORD.toCharArray())  // client certificate
//                .loadTrustMaterial(trustStore, null)               // server CA
//                .build();
//
//        SSLConnectionSocketFactory socketFactory =
//                new SSLConnectionSocketFactory(sslContext);
//
//        // HttpClient using our SSLContext
//        CloseableHttpClient httpClient = HttpClients.custom()
//                .setSSLSocketFactory(socketFactory)
//                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
//                .build();
//
//        // HttpRequestFactory for Eureka
//        HttpComponentsClientHttpRequestFactory requestFactory =
//                new HttpComponentsClientHttpRequestFactory(httpClient);
//
//        // Wrap in a supplier
//        EurekaClientHttpRequestFactorySupplier supplier =
//                (sslContextIn, hostnameVerifierIn) -> {
//                    // You can optionally use sslContext & hostnameVerifier to build a custom HttpClient
//                    return new HttpComponentsClientHttpRequestFactory(httpClient);
//                };
//
//        // Supply the factory to Eureka
//        return new RestTemplateDiscoveryClientOptionalArgs(supplier);
//
//    }
//}
//
