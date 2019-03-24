package org.outreach.outreachfeedbackauthserver;

import org.outreach.outreachfeedbackauthserver.entities.Role;
import org.outreach.outreachfeedbackauthserver.entities.User;
import org.outreach.outreachfeedbackauthserver.services.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

@SpringBootApplication
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
            service.save(new User(
                    "admin@admin.com", // email
                    "admin",            //password
                    "admin",            //username
                    true,
                    true,
                    true,
                    true,
                    Arrays.asList(new Role("ROLE_ADMIN"))
            ));
            service.save(new User(
                    "user@admin.com", // email
                    "user",            //password
                    "user",            //username
                    true,
                    true,
                    true,
                    true,
                    Arrays.asList(new Role("ROLE_USER"))
            ));
        };

    }
}
