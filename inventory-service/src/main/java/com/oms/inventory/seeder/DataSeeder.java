package com.oms.inventory.seeder;

import com.github.javafaker.Faker;
import com.oms.inventory.entity.Inventory;
import com.oms.inventory.repository.InventoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class DataSeeder implements ApplicationRunner {

    private final InventoryRepository inventoryRepo;

    public DataSeeder(InventoryRepository inventoryRepo) {
        this.inventoryRepo = inventoryRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        Faker faker = new Faker();

        if (inventoryRepo.count() == 0) {
            int maxProductId = 25;
            List<Inventory> stocks = IntStream.range(0, maxProductId)
                    .mapToObj(i-> Inventory.builder()
                            .productId((long) (i+1))
                            .stockQuantity(Integer.valueOf(faker.number().numberBetween(25, 100)))
                            .inStock(Boolean.TRUE)
                            .reservedQuantity(0)
                            .lastUpdated(LocalDateTime.now())
                            .build()
                    )
                    .toList();

           inventoryRepo.saveAll(stocks);
        }

    }
}

