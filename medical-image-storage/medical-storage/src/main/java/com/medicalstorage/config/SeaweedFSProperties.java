package com.medicalstorage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds all seaweedfs.* properties from application.yml.
 * Injected wherever SeaweedFS URLs are needed.
 */
@Component
@ConfigurationProperties(prefix = "seaweedfs")
@Getter
@Setter
public class SeaweedFSProperties {

    /** Master node URL, e.g. http://localhost:9333 */
    private String masterUrl;

    /** Volume node URL, e.g. http://localhost:8080
     *  (will be overridden by the URL returned from /dir/assign) */
    private String volumeUrl;

    /** Path for the assign endpoint */
    private String assignEndpoint;

    /** Replication mode, e.g. "001" or blank for default */
    private String replication;
}
