package org.ria.ifzz.RiaApp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class RiaAppApplication {
//
//	@Bean
//	BCryptPasswordEncoder bCryptPasswordEncoder(){
//		return new BCryptPasswordEncoder();
//	}

	public static void main(String[] args) {
		SpringApplication.run(RiaAppApplication.class, args);
	}

}
