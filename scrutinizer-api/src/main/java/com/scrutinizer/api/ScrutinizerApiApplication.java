package com.scrutinizer.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.scrutinizer.api",
    "com.scrutinizer.engine",
    "com.scrutinizer.enrichment",
    "com.scrutinizer.parser",
    "com.scrutinizer.policy",
    "com.scrutinizer.graph"
})
@EntityScan(basePackages = "com.scrutinizer.api.entity")
public class ScrutinizerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScrutinizerApiApplication.class, args);
    }
}
