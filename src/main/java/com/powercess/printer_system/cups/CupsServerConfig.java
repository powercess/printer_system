package com.powercess.printer_system.cups;

/**
 * CUPS 服务器配置
 */
public record CupsServerConfig(
    String id,
    String host,
    int port,
    String username,
    String password,
    boolean secure,
    int timeout,
    int priority
) {
    public CupsServerConfig {
        if (id == null || id.isEmpty()) {
            id = host + ":" + port;
        }
        port = port > 0 ? port : 631;
        timeout = timeout > 0 ? timeout : 30000;
        priority = priority >= 0 ? priority : 0;
    }

    public String getIppUrl() {
        String protocol = secure ? "https" : "http";
        return protocol + "://" + host + ":" + port;
    }

    public boolean hasAuth() {
        return username != null && !username.isEmpty() && password != null && !password.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String host = "localhost";
        private int port = 631;
        private String username;
        private String password;
        private boolean secure = false;
        private int timeout = 30000;
        private int priority = 0;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder secure(boolean secure) {
            this.secure = secure;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public CupsServerConfig build() {
            return new CupsServerConfig(id, host, port, username, password, secure, timeout, priority);
        }
    }
}