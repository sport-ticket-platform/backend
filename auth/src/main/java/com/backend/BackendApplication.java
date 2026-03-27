package com.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import redis.clients.jedis.Jedis;

@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(BackendApplication.class, args);
    }
}