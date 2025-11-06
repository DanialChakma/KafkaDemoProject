package com.oms.utils;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class ORDGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();


    public static String genOrdNumber() {
        String datePart = LocalDate.now().format(DATE_FORMAT);

        // Get microseconds (or just a slice of nanoseconds)
        long micros = System.nanoTime() % 1000000; // 6 digits
        int randomPart = RANDOM.nextInt(1000);       // 3 more random digits

        String suffix = String.format("%06d%03d", micros, randomPart);
        return String.format("ORD-%s-%s", datePart, suffix);
    }
}


