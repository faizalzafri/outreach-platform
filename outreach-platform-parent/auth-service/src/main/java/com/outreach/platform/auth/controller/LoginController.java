package com.outreach.platform.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the custom Thymeleaf login page for the Authorization Server.
 * Spring Security's {@code .loginPage("/login").permitAll()} ensures this
 * endpoint is accessible without authentication.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}
