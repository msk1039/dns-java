package com.mayank.dns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DnsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DnsApplication.class, args);
	}

}
