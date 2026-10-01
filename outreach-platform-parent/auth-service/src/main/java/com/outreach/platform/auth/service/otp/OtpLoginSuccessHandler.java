package com.outreach.platform.auth.service.otp;

import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.model.OtpPurpose;
import com.outreach.platform.auth.repo.UserAccountRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.RequestCache;

import java.io.IOException;
import java.util.Optional;

/**
 * Runs after a correct password. If the user's policy wants a one-time code at sign-in, the
 * authentication is parked in the session (not in the security context, so the user is still
 * anonymous), a code is sent, and the browser goes to {@code /login/otp}. Otherwise sign-in
 * completes as usual and resumes the saved authorization request.
 */
@Named
public class OtpLoginSuccessHandler implements AuthenticationSuccessHandler {

    /** Session attribute holding the authentication that a correct code will complete. */
    public static final String PENDING_AUTHENTICATION = OtpLoginSuccessHandler.class.getName() + ".PENDING";

    private final UserAccountRepository accounts;
    private final OtpService otp;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
    private final SavedRequestAwareAuthenticationSuccessHandler resume = new SavedRequestAwareAuthenticationSuccessHandler();

    @Inject
    public OtpLoginSuccessHandler(UserAccountRepository accounts, OtpService otp, RequestCache requestCache) {
        this.accounts = accounts;
        this.otp = otp;
        this.resume.setRequestCache(requestCache);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, jakarta.servlet.ServletException {
        Optional<UserAccount> account = accounts.findByUsername(authentication.getName());
        if (account.isEmpty() || !otp.isRequired(account.get(), OtpPurpose.LOGIN)) {
            resume.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        // Undo what the login filter just saved: the user is not signed in until the code checks out.
        SecurityContextHolder.clearContext();
        contextRepository.saveContext(SecurityContextHolder.createEmptyContext(), request, response);

        try {
            otp.issue(account.get(), OtpPurpose.LOGIN);
        } catch (OtpService.OtpCooldownException alreadySent) {
            // A code went out moments ago (double submit); the page offers a resend once allowed.
        } catch (IllegalStateException unreachable) {
            // Fail closed: the policy demands a code and there is nowhere to send one.
            response.sendRedirect(request.getContextPath() + "/login?otpUnavailable");
            return;
        }
        request.getSession().setAttribute(PENDING_AUTHENTICATION, authentication);
        response.sendRedirect(request.getContextPath() + "/login/otp");
    }

    /** Completes a parked sign-in once its code is accepted, then resumes the saved request. */
    public void complete(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, jakarta.servlet.ServletException {
        request.getSession().removeAttribute(PENDING_AUTHENTICATION);
        request.changeSessionId(); // a new session id for the fully signed-in session
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        resume.onAuthenticationSuccess(request, response, authentication);
    }
}
