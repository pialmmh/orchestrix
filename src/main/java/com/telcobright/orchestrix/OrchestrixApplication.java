package com.telcobright.orchestrix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.telcobright.orchestrix", "com.orchestrix.stellar"})
public class OrchestrixApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrchestrixApplication.class, args);
    }
}