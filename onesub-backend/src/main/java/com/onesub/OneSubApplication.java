package com.onesub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableScheduling
@RestController
public class OneSubApplication {

	public static void main(String[] args) {
		SpringApplication.run(OneSubApplication.class, args);
	}

	@GetMapping("/")
	public java.util.Map<String, Object> rootStatus() {
		return java.util.Map.of(
				"application", "OneSub",
				"status", "running",
				"api", "/api/v1"
		);
	}

}
