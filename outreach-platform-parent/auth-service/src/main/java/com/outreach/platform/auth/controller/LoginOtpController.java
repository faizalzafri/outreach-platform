package com.outreach.platform.auth.controller;

import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.model.OtpPurpose;
import com.outreach.platform.auth.repo.UserAccountRepository;
import com.outreach.platform.auth.service.otp.OtpLoginSuccessHandler;
import com.outreach.platform.auth.service.otp.OtpSender;
import com.outreach.platform.auth.service.otp.OtpService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * The second sign-in step: enter the one-time code sent after a correct password. Only a session
 * holding a parked authentication (see {@link OtpLoginSuccessHandler}) can use it.
 */
@Controller
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
public class LoginOtpController {

    private final OtpService otp;
    private final OtpLoginSuccessHandler loginHandler;
    private final UserAccountRepository accounts;
    private final List<OtpSender> senders;

    @Inject
    public LoginOtpController(OtpService otp, OtpLoginSuccessHandler loginHandler,
                              UserAccountRepository accounts, List<OtpSender> senders) {
        this.otp = otp;
        this.loginHandler = loginHandler;
        this.accounts = accounts;
        this.senders = senders;
    }

    @GetMapping("/login/otp")
    public String form(HttpSession session, Model model) {
        Optional<UserAccount> account = pendingAccount(session);
        if (account.isEmpty()) {
            return "redirect:/login";
        }
        return page(account.get(), model, null, null);
    }

    @PostMapping("/login/otp")
    public String verify(@RequestParam String code, HttpServletRequest request, HttpServletResponse response,
                         Model model) throws IOException, ServletException {
        HttpSession session = request.getSession();
        Optional<UserAccount> account = pendingAccount(session);
        if (account.isEmpty()) {
            return "redirect:/login";
        }
        OtpService.Verification result = otp.verify(account.get(), OtpPurpose.LOGIN, code);
        switch (result.outcome()) {
            case ACCEPTED -> {
                loginHandler.complete(request, response, (Authentication) session.getAttribute(
                        OtpLoginSuccessHandler.PENDING_AUTHENTICATION));
                return null; // the handler has redirected to the saved authorization request
            }
            case WRONG -> {
                return page(account.get(), model,
                        "That code isn't right. " + result.attemptsLeft()
                                + (result.attemptsLeft() == 1 ? " attempt" : " attempts") + " left.", null);
            }
            default -> {
                session.removeAttribute(OtpLoginSuccessHandler.PENDING_AUTHENTICATION);
                return "redirect:/login?otpExpired";
            }
        }
    }

    @PostMapping("/login/otp/resend")
    public String resend(HttpSession session, Model model) {
        Optional<UserAccount> account = pendingAccount(session);
        if (account.isEmpty()) {
            return "redirect:/login";
        }
        try {
            otp.issue(account.get(), OtpPurpose.LOGIN);
            return page(account.get(), model, null, "We sent you a new code.");
        } catch (OtpService.OtpCooldownException e) {
            return page(account.get(), model, e.getMessage() + ".", null);
        }
    }

    private Optional<UserAccount> pendingAccount(HttpSession session) {
        Object pending = session.getAttribute(OtpLoginSuccessHandler.PENDING_AUTHENTICATION);
        return pending instanceof Authentication authentication
                ? accounts.findByUsername(authentication.getName())
                : Optional.empty();
    }

    private String page(UserAccount account, Model model, String error, String notice) {
        model.addAttribute("destination", senders.stream()
                .filter(s -> s.canReach(account))
                .findFirst()
                .map(s -> s.maskedDestination(account))
                .orElse("your registered contact"));
        model.addAttribute("error", error);
        model.addAttribute("notice", notice);
        return "login-otp";
    }
}
