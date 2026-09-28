package com.tup.reconac;

import com.tup.reconac.config.security.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class ReconacApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReconacApplication.class, args);
	}
}
