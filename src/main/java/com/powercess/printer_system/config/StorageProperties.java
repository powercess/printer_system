package com.powercess.printer_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
    String type,
    Local local,
    S3 s3
) {
    public StorageProperties {
        type = type != null ? type : "local";
        local = local != null ? local : new Local(null);
        s3 = s3 != null ? s3 : new S3(null, null, null, null, null, false, 3600);
    }

    public boolean isS3Enabled() {
        return "s3".equalsIgnoreCase(type);
    }

    public record Local(
        String dir
    ) {
        public Local {
            dir = dir != null ? dir : "./uploads";
        }
    }

    public record S3(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        boolean pathStyle,
        int presignedUrlExpiration
    ) {
        public S3 {
            endpoint = endpoint != null ? endpoint : "https://s3.amazonaws.com";
            region = region != null ? region : "us-east-1";
            presignedUrlExpiration = presignedUrlExpiration > 0 ? presignedUrlExpiration : 3600;
        }
    }
}