package com.outreach.platform.common.pii;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiBlindIndexTest {

    @BeforeAll
    static void setKey() {
        System.setProperty("pii.encryption.key", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    }

    @Test
    void sameValueInAnyCaseOrSpacing_givesSameIndex_differentValuesDoNot() {
        String index = PiiBlindIndex.of("Priya.Sharma@Example.com");

        assertThat(index).hasSize(64)
                .isEqualTo(PiiBlindIndex.of("  priya.sharma@example.com "))
                .isNotEqualTo(PiiBlindIndex.of("priya.sharma@example.org"));
        assertThat(PiiBlindIndex.of(" ")).isNull();
        assertThat(PiiBlindIndex.of(null)).isNull();
    }
}
