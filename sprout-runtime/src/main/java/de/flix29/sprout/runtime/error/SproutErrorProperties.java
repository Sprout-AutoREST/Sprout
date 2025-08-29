package de.flix29.sprout.runtime.error;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sprout.errors")
public class SproutErrorProperties {

    /**
     * Activate/deactivate the error handling controller advice.
     * If deactivated, Spring Boot's default error handling will be used.
     */
    private boolean enabled = true;

    /**
     * Log stacktraces of exceptions to the application log.
     * These stacktraces are never included in the API response.
     */
    private boolean logStacktraces = false;

    /**
     * Internal error code to be used for server errors (HTTP 500).
     * This is used as a default fallback code for internal errors.
     */
    private String internalCode = "internal_error";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogStacktraces() {
        return logStacktraces;
    }

    public void setLogStacktraces(boolean logStacktraces) {
        this.logStacktraces = logStacktraces;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public void setInternalCode(String internalCode) {
        this.internalCode = internalCode;
    }
}