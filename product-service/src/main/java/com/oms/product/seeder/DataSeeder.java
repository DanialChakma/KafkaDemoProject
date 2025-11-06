package com.oms.product.seeder;

import com.github.javafaker.Faker;
import com.oms.product.entity.Product;
import com.oms.product.repository.ProductRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class DataSeeder implements ApplicationRunner {

    private final ProductRepository productRepo;

    public DataSeeder(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        Faker faker = new Faker();

        if (productRepo.count() == 0) {

           int maxProductId = 25;
           List<Product> products = IntStream.range(0, maxProductId)
                    .mapToObj(i-> Product.builder()
                            .id((long) (i+1))
                            .name(faker.commerce().productName())
                            .skuCode("SKU-" + faker.letterify("???").toUpperCase() + "-" + faker.number().digits(4))
                            .active(Boolean.TRUE)
                            .price( Double.valueOf(faker.commerce().price(25, 125)) )
                            .createdAt(LocalDateTime.now())
                            .description(faker.commerce().productName() + " " + faker.commerce().material() + " for " + faker.commerce().department())
                            .category(faker.commerce().department())
                            .build()
                    )
                    .toList();

           productRepo.saveAll(products);
        }

    }
}

