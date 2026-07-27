package com.inklusport.reports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Este microservicio funciona */

@SpringBootApplication
@EnableFeignClients
public class ReportsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportsApplication.class, args);
    }
}