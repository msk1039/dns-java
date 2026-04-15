package com.mayank.dns.repository;

import com.mayank.dns.protocol.DnsRecord;

import java.time.Instant;
import java.util.List;

/**
 * Represents a single cache entry: the DNS records for a domain + record type,
 * along with their expiration time and the original response code.
 */
public class CacheEntry {

    private final List<DnsRecord> records;
    private final Instant expiresAt;
    private final int responseCode; // 0 = NOERROR, 3 = NXDOMAIN, etc.

    public CacheEntry(List<DnsRecord> records, Instant expiresAt, int responseCode) {
        this.records = records;
        this.expiresAt = expiresAt;
        this.responseCode = responseCode;
    }

    public List<DnsRecord> getRecords() {
        return records;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Returns true if this cache entry has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Returns the remaining TTL in seconds, or 0 if expired.
     */
    public long getRemainingTtlSeconds() {
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    @Override
    public String toString() {
        return "CacheEntry{" +
                "records=" + records +
                ", expiresAt=" + expiresAt +
                ", responseCode=" + responseCode +
                ", expired=" + isExpired() +
                '}';
    }
}
