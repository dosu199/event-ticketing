package com.ticketing.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of booking-service.
 *
 * <p>{@code @SpringBootApplication} combines three annotations:
 * <ul>
 *   <li>{@code @Configuration}: this class can define Spring beans.</li>
 *   <li>{@code @EnableAutoConfiguration}: Spring Boot configures things based on
 *       the jars on the classpath (e.g. Tomcat because the web starter is present).</li>
 *   <li>{@code @ComponentScan}: finds {@code @Component}, {@code @Service},
 *       {@code @RestController}... in this package and all sub-packages.
 *       That's why this class sits at the ROOT package {@code com.ticketing.booking},
 *       above the api/domain/service/... sub-packages.</li>
 * </ul>
 */
@SpringBootApplication
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
