package com.jpoltramari.library_api.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.errors")
public class ErrorProperties {

    private boolean exposeDetails = false;

    public boolean isExposeDetails() {
        return exposeDetails;
    }

    public void setExposeDetails(boolean exposeDetails) {
        this.exposeDetails = exposeDetails;
    }
}
