package org.outreach.outreachfeedbackauthserver;

import org.outreach.outreachfeedbackauthserver.entities.Role;
import org.outreach.outreachfeedbackauthserver.entities.User;
import org.outreach.outreachfeedbackauthserver.services.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableEurekaClient
public class OutreachFeedbackAuthServer {

	public static void main(String[] args) {
		SpringApplication.run(OutreachFeedbackAuthServer.class, args);

	}

	@Bean
	public static PasswordEncoder getPasswordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public CommandLineRunner setupDefaultUser(UserService service) {
		return args -> {
			service.save(new User("admin@admin.com", // email
					"111", // username
					"admin", // password
					true, true, true, true, Role.ROLE_ADMIN));
			service.save(new User("poc@poc.com", // email
					"222", // username
					"poc", // username // password
					true, true, true, true, Role.ROLE_POC));
			service.save(new User("pmo@pmo.com", // email
					"333", // username
					"pmo", // password
					true, true, true, true, Role.ROLE_PMO));
		};

	}
}
