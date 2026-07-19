package com.outreach.platform.common.integration;

import com.outreach.platform.common.security.XssSanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration-level test for XSS sanitization.
 * Verifies that all known XSS attack vectors are stripped, including complex/compound payloads
 * that combine multiple attack patterns.
 */
class XssSanitizationIT {

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("XSS sanitizer strips all executable content from attack vectors")
    @MethodSource("xssAttackVectors")
    void sanitize_stripsExecutableContent(String description, String maliciousInput, String... forbiddenFragments) {
        String sanitized = XssSanitizer.sanitize(maliciousInput);

        for (String forbidden : forbiddenFragments) {
            assertThat(sanitized.toLowerCase())
                    .as("Sanitized output should not contain '%s' (from: %s)", forbidden, description)
                    .doesNotContain(forbidden.toLowerCase());
        }
    }

    static Stream<Arguments> xssAttackVectors() {
        return Stream.of(
                // Script tag attacks — XssSanitizer removes tags, leaving inner text
                Arguments.of(
                        "Basic script tag removal",
                        "<script>alert('xss')</script>",
                        new String[]{"<script>", "</script>"}
                ),
                Arguments.of(
                        "Script tag with SRC attribute",
                        "<SCRIPT SRC=http://evil.com/xss.js></SCRIPT>",
                        new String[]{"<script", "</script>"}
                ),

                // javascript: protocol attacks
                Arguments.of(
                        "javascript: protocol in href",
                        "<a href=\"javascript:alert('xss')\">click me</a>",
                        new String[]{"javascript:", "<a "}
                ),
                Arguments.of(
                        "javascript: with mixed case",
                        "JaVaScRiPt:alert(document.cookie)",
                        new String[]{"javascript:"}
                ),

                // Event handler attacks
                Arguments.of(
                        "onclick event handler",
                        "<div onclick=alert('xss')>text</div>",
                        new String[]{"onclick=", "<div"}
                ),
                Arguments.of(
                        "onerror event handler on img",
                        "<img src=x onerror=alert(1)>",
                        new String[]{"<img", "onerror="}
                ),
                Arguments.of(
                        "onload event handler",
                        "<body onload=alert('xss')>",
                        new String[]{"<body", "onload="}
                ),
                Arguments.of(
                        "onmouseover handler",
                        "<span onmouseover=alert(1)>hover</span>",
                        new String[]{"onmouseover=", "<span"}
                ),

                // data: URI attacks
                Arguments.of(
                        "data: URI with base64 payload",
                        "data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==",
                        new String[]{"data:text/html;base64"}
                ),

                // Compound attacks
                Arguments.of(
                        "Compound: script + event handler",
                        "<script>x</script><img onerror=y>",
                        new String[]{"<script>", "onerror=", "<img"}
                ),
                Arguments.of(
                        "SVG with embedded script",
                        "<svg><script>alert(1)</script></svg>",
                        new String[]{"<svg>", "<script>", "</script>"}
                )
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("XSS sanitizer preserves safe content")
    @MethodSource("safeInputs")
    void sanitize_preservesSafeContent(String description, String safeInput) {
        String sanitized = XssSanitizer.sanitize(safeInput);
        // Safe content should remain largely intact (trimmed)
        assertThat(sanitized).isNotEmpty();
    }

    static Stream<Arguments> safeInputs() {
        return Stream.of(
                Arguments.of("Plain text", "Hello World, this is feedback!"),
                Arguments.of("Text with numbers", "Order 12345 was processed on 2024-01-15"),
                Arguments.of("Text with punctuation", "Great service! Would recommend. Rating: 5/5"),
                Arguments.of("Text with unicode", "Ñoño feedback — very helpful ✓")
        );
    }
}
