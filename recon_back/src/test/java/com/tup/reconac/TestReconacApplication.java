package com.tup.reconac;

import org.springframework.boot.SpringApplication;

public class TestReconacApplication {

	public static void main(String[] args) {
		SpringApplication.from(ReconacApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
