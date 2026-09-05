package com.indomaret.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.indomaret.backend.entity.User;
import com.indomaret.backend.repository.UserRepository;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner initDefaultUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			userRepository.findByUsernameAndIsActiveTrue("admin").ifPresentOrElse(
				admin -> {
					admin.setPasswordHash(passwordEncoder.encode("password123"));
					userRepository.save(admin);
				},
				() -> {
					User admin = new User();
					admin.setUsername("admin");
					admin.setPasswordHash(passwordEncoder.encode("password123"));
					admin.setRole("ADMIN");
					admin.setIsActive(true);
					userRepository.save(admin);
				}
			);
		};
	}

}
