package com.example.zylo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.zylo.**.repository")
public class ZyloApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZyloApplication.class, args);
    }

}
