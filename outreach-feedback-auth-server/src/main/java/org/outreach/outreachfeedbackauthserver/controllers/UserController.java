package org.outreach.outreachfeedbackauthserver.controllers;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

import org.outreach.outreachfeedbackauthserver.entities.User;
import org.outreach.outreachfeedbackauthserver.model.LoginResponseModel;
import org.outreach.outreachfeedbackauthserver.model.UserResponseModel;
import org.outreach.outreachfeedbackauthserver.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping("/login")
    public LoginResponseModel login(HttpServletRequest request) {

        String[] credentials = filterCredentials(request);
        User user = userService.getUserByCredentials(credentials);
        LoginResponseModel loginResponseModel = new LoginResponseModel();
        loginResponseModel.setUserAuthentic(userService.authenticate(credentials));
        return loginResponseModel;
    }

    @RequestMapping("/user")
    public UserResponseModel authenticate(HttpServletRequest request) {
        String[] credentials = filterCredentials(request);
        User user = userService.getUserByCredentials(credentials);
        UserResponseModel userResponseModel = new UserResponseModel(user.getUsername(), user.getUsername(), user.getEmail(), user.getRoles().get(0).getName());
        return userResponseModel;
    }

    private String[] filterCredentials(HttpServletRequest request) {
        String authToken = request.getHeader("Authorization").substring("Basic".length()).trim();
        return new String(Base64.getDecoder().decode(authToken)).split(":");
    }
}