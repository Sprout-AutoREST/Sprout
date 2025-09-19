package de.flix29.sprout.runtime.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@AutoConfiguration
@ConditionalOnClass(EnableMethodSecurity.class)
@EnableConfigurationProperties(SproutMethodSecurityProperties.class)
@ConditionalOnProperty(prefix = "sprout.security.method-security", name = "enabled", havingValue = "true")
@EnableMethodSecurity
public class SproutMethodSecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SproutMethodSecurityConfiguration.class);

    SproutMethodSecurityConfiguration() {
        log.info("[Sprout] Method Security enabled via sprout.security.method-security.enabled=true");
    }
}