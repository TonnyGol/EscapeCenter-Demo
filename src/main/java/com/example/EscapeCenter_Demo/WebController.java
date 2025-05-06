package com.example.EscapeCenter_Demo;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class WebController {
    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

}
