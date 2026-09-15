package com.outreach.platform.common.logging;

import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;

import java.io.IOException;

/**
 * JsonGeneratorDecorator that applies PII and secret masking to all string values in JSON log output.
 */
public class MaskingJsonGeneratorDecorator implements JsonGeneratorDecorator {

    @Override
    public JsonGenerator decorate(JsonGenerator generator) {
        return new MaskingJsonGenerator(generator);
    }

    /** Delegating JsonGenerator that applies masking to string values. */
    private static class MaskingJsonGenerator extends com.fasterxml.jackson.core.util.JsonGeneratorDelegate {

        public MaskingJsonGenerator(JsonGenerator delegate) {
            super(delegate);
        }

        @Override
        public void writeString(String text) throws IOException {
            if (text != null) {
                text = PiiMaskingMessageConverter.maskPii(text);
                text = SecretMaskingMessageConverter.maskSecrets(text);
            }
            super.writeString(text);
        }

        @Override
        public void writeString(char[] text, int offset, int len) throws IOException {
            String value = new String(text, offset, len);
            writeString(value);
        }
    }
}
