package com.oms.customer.seeder;

import com.github.javafaker.Faker;
import com.oms.customer.entity.Address;
import com.oms.customer.entity.Customer;
import com.oms.customer.repository.CustomerRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Component
public class DataSeeder implements ApplicationRunner {

    private final CustomerRepository customerRepo;

    public DataSeeder(CustomerRepository customerRepo) {
        this.customerRepo = customerRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        Faker faker = new Faker();

        if (customerRepo.count() == 0) {
           List<Customer> customers = IntStream.range(0, 25)
                    .mapToObj(i->Customer.builder()
//                            .id(UUID.randomUUID())
                            .firstName(faker.name().firstName())
                            .lastName(faker.name().lastName())
                            .email(faker.internet().emailAddress())
                            .phone(faker.phoneNumber().cellPhone())
                            .addresses(
                                    List.of(
                                        Address.builder()
                                        .street(faker.address().streetAddress())
                                        .city(faker.address().city())
                                        .postalCode(faker.address().zipCode())
                                        .country(faker.address().country())
                                        .state(faker.address().state())
                                        .build()
                                    )

                            )
                            .build()
                    )
                    .toList();

           customerRepo.saveAll(customers);
        }

    }
}

