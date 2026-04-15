package com.mayank.dns.repository;

import com.mayank.dns.protocol.DnsRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CacheEntry — expiration, TTL, and basic getters.
 */
class CacheEntryTest {

    private DnsRecord sampleRecord() {
        return new DnsRecord("example.com", 1, 1, 300, new byte[]{1, 2, 3, 4});
    }

    @Test
    @DisplayName("isExpired returns false for future expiry")
    void isExpiredReturnsFalseForFutureExpiry() {
        CacheEntry entry = new CacheEntry(
                List.of(sampleRecord()),
                Instant.now().plusSeconds(600),
                0
        );
        assertFalse(entry.isExpired());
    }

    @Test
    @DisplayName("isExpired returns true for past expiry")
    void isExpiredReturnsTrueForPastExpiry() {
        CacheEntry entry = new CacheEntry(
                List.of(sampleRecord()),
                Instant.now().minusSeconds(10),
                0
        );
        assertTrue(entry.isExpired());
    }

    @Test
    @DisplayName("getRemainingTtlSeconds returns positive value for future expiry")
    void remainingTtlPositiveForFutureExpiry() {
        CacheEntry entry = new CacheEntry(
                List.of(sampleRecord()),
                Instant.now().plusSeconds(120),
                0
        );
        long remaining = entry.getRemainingTtlSeconds();
        assertTrue(remaining > 0 && remaining <= 120);
    }

    @Test
    @DisplayName("getRemainingTtlSeconds returns 0 for expired entry")
    void remainingTtlZeroForExpiredEntry() {
        CacheEntry entry = new CacheEntry(
                List.of(sampleRecord()),
                Instant.now().minusSeconds(60),
                0
        );
        assertEquals(0, entry.getRemainingTtlSeconds());
    }

    @Test
    @DisplayName("Getters return constructor values")
    void gettersReturnConstructorValues() {
        List<DnsRecord> records = List.of(sampleRecord());
        Instant expiresAt = Instant.now().plusSeconds(300);
        CacheEntry entry = new CacheEntry(records, expiresAt, 3);

        assertEquals(records, entry.getRecords());
        assertEquals(expiresAt, entry.getExpiresAt());
        assertEquals(3, entry.getResponseCode());
    }

    @Test
    @DisplayName("NXDOMAIN entry (responseCode=3) stores empty records list")
    void nxdomainEntryHasEmptyRecords() {
        CacheEntry entry = new CacheEntry(List.of(), Instant.now().plusSeconds(300), 3);

        assertTrue(entry.getRecords().isEmpty());
        assertEquals(3, entry.getResponseCode());
    }

    @Test
    @DisplayName("toString contains relevant info")
    void toStringContainsInfo() {
        CacheEntry entry = new CacheEntry(List.of(sampleRecord()), Instant.now().plusSeconds(300), 0);
        String str = entry.toString();

        assertTrue(str.contains("CacheEntry"));
        assertTrue(str.contains("responseCode=0"));
    }
}
