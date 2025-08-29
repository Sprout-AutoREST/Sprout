package de.flix29.sprout.runtime.error;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@AutoConfiguration
@EnableConfigurationProperties(SproutErrorProperties.class)
@ConditionalOnClass(RestControllerAdvice.class)
@ConditionalOnProperty(prefix = "sprout.errors", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SproutErrorConfiguration {

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    GlobalExceptionHandler sproutGlobalExceptionHandler(SproutErrorProperties props) {
        return new GlobalExceptionHandler(props);
    }
}