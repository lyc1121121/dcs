package com.dcsagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DcsAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DcsAgentApplication.class, args);
    }
}
