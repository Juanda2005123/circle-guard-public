package com.circleguard.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.features")
public class FeatureFlagsProperties {

    private boolean visitorHandoffEnabled = true;

    public boolean isVisitorHandoffEnabled() {
        return visitorHandoffEnabled;
    }

    public void setVisitorHandoffEnabled(boolean visitorHandoffEnabled) {
        this.visitorHandoffEnabled = visitorHandoffEnabled;
    }
}
