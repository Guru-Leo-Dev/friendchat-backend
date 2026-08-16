package com.friendchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FriendChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(FriendChatApplication.class, args);
    }
}
