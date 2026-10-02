package com.outreach.platform.auth.controller;

import com.outreach.platform.auth.config.AuthServiceProperties;
import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.model.AccountTokenPurpose;
import com.outreach.platform.auth.service.PasswordService.PasswordPolicyViolationException;
import com.outreach.platform.auth.service.SecurityPolicyService;
import com.outreach.platform.auth.service.SelfServiceAccountService;
import com.outreach.platform.auth.service.SelfServiceAccountService.InvalidLinkException;
import com.outreach.platform.auth.model.OtpPurpose;
import com.outreach.platform.auth.service.otp.OtpService;
import jakarta.inject.Inject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

/**
 * The auth server's own account pages, reached from emailed links or the sign-in page: activate
 * an invitation, request a password reset, and choose a new password. All are public; the
 * single-use token in the link is the credential.
 */
@Controller
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
public class AccountPagesController {

    private final SelfServiceAccountService selfService;
    private final SecurityPolicyService policies;
    private final AuthServiceProperties properties;
    private final OtpService otp;

    @Inject
    public AccountPagesController(SelfServiceAccountService selfService, SecurityPolicyService policies,
                                  AuthServiceProperties properties, OtpService otp) {
        this.selfService = selfService;
        this.policies = policies;
        this.properties = properties;
        this.otp = otp;
    }

    @GetMapping("/activate")
    public String activationForm(@RequestParam(required = false) String token, Model model) {
        return linkForm(token, AccountTokenPurpose.ACTIVATION, List.of(), model);
    }

    @PostMapping("/activate")
    public String activate(@RequestParam String token, @RequestParam String password,
                           @RequestParam String confirmPassword, Model model) {
        return completeLink(token, AccountTokenPurpose.ACTIVATION, password, confirmPassword, model,
                "redirect:/login?activated");
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestReset(@RequestParam String identifier, Model model) {
        selfService.requestPasswordReset(identifier);
        model.addAttribute("sent", true);
        model.addAttribute("resetMinutes", properties.accounts().passwordResetTtl().toMinutes());
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        return linkForm(token, AccountTokenPurpose.PASSWORD_RESET, List.of(), model);
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String password,
                                @RequestParam String confirmPassword,
                                @RequestParam(required = false) String otpCode, Model model) {
        Optional<UserAccount> account = selfService.accountForLink(token, AccountTokenPurpose.PASSWORD_RESET);
        if (account.isPresent() && otp.isRequired(account.get(), OtpPurpose.PASSWORD_RESET)) {
            OtpService.Verification check = otp.verify(account.get(), OtpPurpose.PASSWORD_RESET, otpCode);
            if (!check.accepted()) {
                String problem = check.outcome() == OtpService.Outcome.WRONG
                        ? "That verification code isn't right (" + check.attemptsLeft() + " attempts left)"
                        : "That verification code expired; we've sent you a new one";
                return linkForm(token, AccountTokenPurpose.PASSWORD_RESET, List.of(problem), model);
            }
        }
        return completeLink(token, AccountTokenPurpose.PASSWORD_RESET, password, confirmPassword, model,
                "redirect:/login?reset");
    }

    private String completeLink(String token, AccountTokenPurpose purpose, String password,
                                String confirmPassword, Model model, String onSuccess) {
        if (!password.equals(confirmPassword)) {
            return linkForm(token, purpose, List.of("The two passwords don't match"), model);
        }
        try {
            selfService.completeLink(token, purpose, password);
            return onSuccess;
        } catch (PasswordPolicyViolationException e) {
            return linkForm(token, purpose, e.violations(), model);
        } catch (InvalidLinkException e) {
            return invalidLink(purpose, model);
        }
    }

    private String linkForm(String token, AccountTokenPurpose purpose, List<String> violations, Model model) {
        Optional<UserAccount> account = selfService.accountForLink(token, purpose);
        if (account.isEmpty()) {
            return invalidLink(purpose, model);
        }
        model.addAttribute("token", token);
        model.addAttribute("username", account.get().getUsername());
        model.addAttribute("displayName", account.get().getDisplayName());
        model.addAttribute("minLength", policies.passwordMinLength(account.get()));
        model.addAttribute("violations", violations);
        if (purpose == AccountTokenPurpose.PASSWORD_RESET && otp.isRequired(account.get(), OtpPurpose.PASSWORD_RESET)) {
            // The link alone isn't enough under this policy: send a code (once, not on every reload).
            try {
                otp.issueIfNoneOpen(account.get(), OtpPurpose.PASSWORD_RESET);
            } catch (OtpService.OtpCooldownException justSent) {
                // a code is already on its way
            }
            model.addAttribute("otpRequired", true);
        }
        return purpose == AccountTokenPurpose.ACTIVATION ? "activate" : "reset-password";
    }

    private static String invalidLink(AccountTokenPurpose purpose, Model model) {
        model.addAttribute("activation", purpose == AccountTokenPurpose.ACTIVATION);
        return "link-invalid";
    }
}
