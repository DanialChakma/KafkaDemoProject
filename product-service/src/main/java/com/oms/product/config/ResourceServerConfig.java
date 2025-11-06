//package com.auth.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.core.io.Resource;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.oauth2.jwt.JwtDecoder;
//import org.springframework.security.oauth2.jwt.JwtException;
//import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
//import org.springframework.security.web.SecurityFilterChain;
//
//import java.nio.file.Files;
//import java.security.KeyFactory;
//import java.security.interfaces.RSAPublicKey;
//import java.security.spec.X509EncodedKeySpec;
//import java.util.Base64;
//
//@Configuration
//@EnableWebSecurity
//public class ResourceServerConfig {
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .authorizeHttpRequests(authorize -> authorize
//                        .anyRequest().authenticated()
//                )
//                .oauth2ResourceServer(oauth2 -> oauth2
//                        .jwt()
//                );
//        return http.build();
//    }
//
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
//        http
//                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
//                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));
//
//        return http.build();
//    }
//
//    @Bean
//    public JwtDecoder jwkSetJwtDecoder() {
//        return NimbusJwtDecoder.withJwkSetUri("http://localhost:9003/oauth/token_key").build();
//    }
//
//    @Bean
//    public JwtDecoder staticKeyJwtDecoder() throws Exception {
//        Resource resource = new ClassPathResource("public-key.cer");
//        String publicKeyContent = new String(Files.readAllBytes(resource.getFile().toPath()))
//                .replaceAll("-----BEGIN PUBLIC KEY-----", "")
//                .replaceAll("-----END PUBLIC KEY-----", "")
//                .replaceAll("\\s+", "");
//        byte[] decoded = Base64.getDecoder().decode(publicKeyContent);
//        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
//        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
//
//        return NimbusJwtDecoder.withPublicKey(publicKey).build();
//    }
//
//    @Bean
//    public JwtDecoder jwtDecoder() throws Exception {
//        JwtDecoder jwkDecoder = jwkSetJwtDecoder();
//        JwtDecoder staticDecoder = staticKeyJwtDecoder();
//
//        return token -> {
//            try {
//                return jwkDecoder.decode(token);
//            } catch (JwtException e) {
//                // If JWKS fails, fallback to static key decoder
//                return staticDecoder.decode(token);
//            }
//        };
//    }
//
//}
//
