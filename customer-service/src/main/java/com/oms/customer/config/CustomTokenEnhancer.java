//package com.ims.config;
//
//import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
//import org.springframework.security.oauth2.common.OAuth2AccessToken;
//import org.springframework.security.oauth2.provider.OAuth2Authentication;
//import org.springframework.security.oauth2.provider.token.TokenEnhancer;
//
//import java.security.SecureRandom;
//
//public class CustomTokenEnhancer implements TokenEnhancer {
//
//    private final SecureRandom random = new SecureRandom();
//
//    private String generateRandomToken(int length) {
//        final String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
//        return random.ints(length, 0, chars.length())
//                .mapToObj(chars::charAt)
//                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
//                .toString();
//    }
//
//    @Override
//    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
//        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(accessToken);
//        token.setValue(generateRandomToken(128)); // Customize token length here
//        return token;
//    }
//}
