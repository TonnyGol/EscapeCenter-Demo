package com.example.EscapeCenter_Demo;

import com.example.EscapeCenter_Demo.gui.ServerGUI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class EscapeCenterDemoApplication {

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "false");

		SpringApplication.run(EscapeCenterDemoApplication.class, args);
		System.out.println("Starting server on http://localhost:8080");

		ServerGUI.launchApp();
	}

}
