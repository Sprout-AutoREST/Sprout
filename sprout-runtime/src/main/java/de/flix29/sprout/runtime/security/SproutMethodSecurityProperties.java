package de.flix29.sprout.runtime.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sprout.security.method-security")
public class SproutMethodSecurityProperties {

    /**
     * Enable @EnableMethodSecurity via Sprout runtime autoconfiguration.
     */
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
