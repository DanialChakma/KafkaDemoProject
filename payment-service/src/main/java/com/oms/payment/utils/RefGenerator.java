package com.oms.payment.utils;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class RefGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates an order number in the format:
     * ORD-YYYYMMDD-<microseconds><random3digits>
     * Example: ORD-20251104-123456789
     */
    public static String genOrdNumber() {
        return generateReference("ORD");
    }

    /**
     * Generates a payment reference in the format:
     * PAY-YYYYMMDD-<microseconds><random3digits>
     * Example: PAY-20251104-654321987
     */
    public static String genPaymentReference() {
        return generateReference("PAY");
    }

    /**
     * Core generator logic for both order and payment references.
     */
    private static String generateReference(String prefix) {
        String datePart = LocalDate.now().format(DATE_FORMAT);

        // Use a combination of system time and random data for uniqueness
        long micros = System.nanoTime() % 1_000_000;  // 6 digits
        int randomPart = RANDOM.nextInt(1_000);       // 3 digits

        String suffix = String.format("%06d%03d", micros, randomPart);
        return String.format("%s-%s-%s", prefix, datePart, suffix);
    }
}

