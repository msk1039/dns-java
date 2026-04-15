package com.mayank.dns.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the DNS server.
 * All properties are prefixed with "dns." in application.properties.
 */
@Configuration
@ConfigurationProperties(prefix = "dns")
public class DnsServerConfig {

    private Server server = new Server();
    private Cache cache = new Cache();

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public static class Server {
        /** UDP port the DNS server listens on */
        private int port = 5354;

        /** Upstream DNS server IP address */
        private String upstream = "8.8.8.8";

        /** Upstream DNS server port */
        private int upstreamPort = 53;

        /** Timeout in milliseconds for upstream DNS queries */
        private int timeoutMs = 3000;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUpstream() {
            return upstream;
        }

        public void setUpstream(String upstream) {
            this.upstream = upstream;
        }

        public int getUpstreamPort() {
            return upstreamPort;
        }

        public void setUpstreamPort(int upstreamPort) {
            this.upstreamPort = upstreamPort;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class Cache {
        /** Maximum number of cached entries */
        private int maxSize = 1000;

        /** Interval in seconds for the TTL cleanup task */
        private int cleanupIntervalSeconds = 60;

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getCleanupIntervalSeconds() {
            return cleanupIntervalSeconds;
        }

        public void setCleanupIntervalSeconds(int cleanupIntervalSeconds) {
            this.cleanupIntervalSeconds = cleanupIntervalSeconds;
        }
    }
}
