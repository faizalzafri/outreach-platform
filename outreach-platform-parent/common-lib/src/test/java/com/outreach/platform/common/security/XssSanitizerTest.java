package com.outreach.platform.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XssSanitizerTest {

    @Test
    void sanitize_nullInput_returnsNull() {
        assertThat(XssSanitizer.sanitize(null)).isNull();
    }

    @Test
    void sanitize_emptyInput_returnsEmpty() {
        assertThat(XssSanitizer.sanitize("")).isEmpty();
    }

    @Test
    void sanitize_plainText_unchanged() {
        String input = "Hello World, this is normal text!";
        assertThat(XssSanitizer.sanitize(input)).isEqualTo(input);
    }

    @Test
    void sanitize_htmlTags_stripped() {
        assertThat(XssSanitizer.sanitize("<script>alert('xss')</script>"))
                .doesNotContain("<script>", "</script>");
    }

    @Test
    void sanitize_imgOnError_stripped() {
        assertThat(XssSanitizer.sanitize("<img src=x onerror=alert(1)>"))
                .doesNotContain("<img", "onerror");
    }

    @Test
    void sanitize_javascriptProtocol_stripped() {
        assertThat(XssSanitizer.sanitize("javascript:alert('xss')"))
                .doesNotContain("javascript:");
    }

    @Test
    void sanitize_eventHandlers_stripped() {
        assertThat(XssSanitizer.sanitize("onmouseover=alert(1)"))
                .doesNotContain("onmouseover=");
    }

    @Test
    void sanitize_encodedHtmlEntities_decoded() {
        assertThat(XssSanitizer.sanitize("&lt;script&gt;"))
                .doesNotContain("&lt;", "&gt;");
    }

    @Test
    void containsXss_cleanInput_returnsFalse() {
        assertThat(XssSanitizer.containsXss("Normal user input")).isFalse();
    }

    @Test
    void containsXss_scriptTag_returnsTrue() {
        assertThat(XssSanitizer.containsXss("<script>alert(1)</script>")).isTrue();
    }

    @Test
    void containsXss_nullInput_returnsFalse() {
        assertThat(XssSanitizer.containsXss(null)).isFalse();
    }
}
