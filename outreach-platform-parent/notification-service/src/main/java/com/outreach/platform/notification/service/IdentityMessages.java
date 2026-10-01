package com.outreach.platform.notification.service;

import com.outreach.platform.notification.channel.OutboundMessage;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Wording of the account emails (invitation, password reset, password changed, one-time code).
 * Every value from the event is HTML-escaped before it reaches the body.
 */
final class IdentityMessages {

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private IdentityMessages() {
    }

    static OutboundMessage invitation(String address, Map<String, Object> p) {
        String name = text(p, "displayName");
        String link = text(p, "activationLink");
        return build(address, name, "You're invited to Outreach Studio",
                "Hi " + name + ",",
                text(p, "invitedBy") + " has invited you to Outreach Studio. Choose a password to activate your account.",
                "Activate your account", link,
                "This link works once and expires on " + when(p, "expiresAt") + ". If you weren't expecting this, you can ignore it.");
    }

    static OutboundMessage passwordReset(String address, Map<String, Object> p) {
        String name = text(p, "displayName");
        return build(address, name, "Reset your Outreach Studio password",
                "Hi " + name + ",",
                "We received a request to reset your password. Use the button below to choose a new one.",
                "Choose a new password", text(p, "resetLink"),
                "This link works once and expires on " + when(p, "expiresAt")
                        + ". If you didn't ask for this, ignore this email; your password stays the same.");
    }

    static OutboundMessage passwordChanged(String address, Map<String, Object> p) {
        String name = text(p, "displayName");
        return build(address, name, "Your Outreach Studio password was changed",
                "Hi " + name + ",",
                "The password for your account was changed on " + when(p, "changedAt") + ".",
                null, null,
                "If this wasn't you, reset your password from the sign-in page right away and tell your administrator.");
    }

    static OutboundMessage oneTimeCode(String address, Map<String, Object> p) {
        String name = text(p, "displayName");
        String code = text(p, "code");
        String action = switch (text(p, "purpose")) {
            case "LOGIN" -> "sign in";
            case "PASSWORD_RESET" -> "reset your password";
            case "PASSWORD_CHANGE" -> "change your password";
            default -> "continue";
        };
        String subject = code + " is your Outreach Studio verification code";
        String html = page("Hi " + esc(name) + ",",
                "<p>Use this code to " + action + ":</p>"
                        + "<p style=\"font-size:28px;font-weight:700;letter-spacing:6px;margin:16px 0\">" + esc(code) + "</p>"
                        + "<p style=\"color:#64748b;font-size:13px\">It expires at " + esc(when(p, "expiresAt"))
                        + ". Never share it; Outreach Studio staff will never ask for it.</p>");
        String text = "Use code " + code + " to " + action + ". It expires at " + when(p, "expiresAt") + ".";
        return new OutboundMessage(address, name, subject, html, text);
    }

    private static OutboundMessage build(String address, String name, String subject, String greeting,
                                         String intro, String buttonLabel, String link, String footnote) {
        StringBuilder body = new StringBuilder("<p>").append(esc(intro)).append("</p>");
        if (link != null) {
            body.append("<p style=\"margin:24px 0\"><a href=\"").append(esc(link))
                    .append("\" style=\"background:#4f46e5;color:#ffffff;padding:12px 20px;border-radius:8px;"
                            + "text-decoration:none;font-weight:600\">").append(esc(buttonLabel)).append("</a></p>")
                    .append("<p style=\"color:#64748b;font-size:13px\">Or paste this link into your browser:<br>")
                    .append(esc(link)).append("</p>");
        }
        body.append("<p style=\"color:#64748b;font-size:13px\">").append(esc(footnote)).append("</p>");
        String text = greeting + "\n\n" + intro + (link != null ? "\n\n" + link : "") + "\n\n" + footnote;
        return new OutboundMessage(address, name, subject, page(esc(greeting), body.toString()), text);
    }

    private static String page(String greeting, String content) {
        return "<div style=\"font-family:system-ui,Segoe UI,Roboto,Arial,sans-serif;max-width:520px;margin:0 auto;"
                + "color:#0f172a;line-height:1.5\"><p style=\"font-weight:600;color:#4f46e5\">◈ Outreach Studio</p>"
                + "<p>" + greeting + "</p>" + content + "</div>";
    }

    private static String text(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "" : value.toString();
    }

    private static String when(Map<String, Object> payload, String key) {
        try {
            return WHEN.format(Instant.parse(text(payload, key)));
        } catch (RuntimeException e) {
            return text(payload, key);
        }
    }

    private static String esc(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
