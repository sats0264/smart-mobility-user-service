package com.mobilitypass.user_mobility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients
public class UserMobilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserMobilityApplication.class, args);
    }

}
