package com.example.EscapeCenter_Server;

import com.example.EscapeCenter_Server.DataBaseService.DefaultService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Objects;


@SpringBootApplication
public class EscapeCenterServer {

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "false");
		// Touching DB_PASS loads DefaultService now, so a missing env var fails at startup instead of on the first request.
		Objects.requireNonNull(DefaultService.DB_PASS);

		SpringApplication.run(EscapeCenterServer.class, args);
		System.out.println("Starting server on http://localhost:8080");

	}

}
