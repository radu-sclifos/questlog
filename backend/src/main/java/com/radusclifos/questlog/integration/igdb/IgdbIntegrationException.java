package com.radusclifos.questlog.integration.igdb;

/**
 * Sanitized: never attach provider response bodies, tokens or credentials.
 */
public class IgdbIntegrationException extends RuntimeException {

    public IgdbIntegrationException(String message) {
        super(message);
    }

}
