package com.example.EscapeCenter_Server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class EscapeCenterServer {

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "false");

		SpringApplication.run(EscapeCenterServer.class, args);
		System.out.println("Starting server on http://localhost:8080");

	}

}
