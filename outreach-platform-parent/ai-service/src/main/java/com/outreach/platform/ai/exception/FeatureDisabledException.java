package com.outreach.platform.ai.exception;

/** Thrown when an AI feature is invoked but its toggle is disabled in configuration. */
public class FeatureDisabledException extends RuntimeException {

    public FeatureDisabledException(String feature) {
        super("AI feature '" + feature + "' is currently disabled");
    }
}
