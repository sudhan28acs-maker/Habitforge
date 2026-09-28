package com.example.habitforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HabitForgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(HabitForgeApplication.class, args);
    }
}
