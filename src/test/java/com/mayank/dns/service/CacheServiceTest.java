package com.mayank.dns.service;

import com.mayank.dns.config.DnsServerConfig;
import com.mayank.dns.model.CacheEntryResponse;
import com.mayank.dns.model.CacheStatsResponse;
import com.mayank.dns.protocol.DnsRecord;
import com.mayank.dns.repository.CacheEntry;
import com.mayank.dns.repository.DnsCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CacheService.
 * Uses Mockito to mock DnsCacheRepository and DnsServerConfig.
 */
@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private DnsCacheRepository cacheRepository;

    private DnsServerConfig config;
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        config = new DnsServerConfig();
        config.getCache().setMaxSize(1000);
        cacheService = new CacheService(cacheRepository, config);
    }

    private CacheEntry createEntry(String domain, long ttlSeconds) {
        DnsRecord record = new DnsRecord(domain, 1, 1, ttlSeconds, new byte[]{10, 0, 0, 1});
        return new CacheEntry(List.of(record), Instant.now().plusSeconds(ttlSeconds), 0);
    }

    @Nested
    @DisplayName("getStats()")
    class GetStatsTests {

        @Test
        @DisplayName("Returns correct statistics")
        void returnsCorrectStats() {
            when(cacheRepository.size()).thenReturn(5);
            when(cacheRepository.getHitCount()).thenReturn(80L);
            when(cacheRepository.getMissCount()).thenReturn(20L);

            CacheStatsResponse stats = cacheService.getStats();

            assertEquals(5, stats.getTotalEntries());
            assertEquals(80, stats.getHits());
            assertEquals(20, stats.getMisses());
            assertEquals(80.0, stats.getHitRate());
            assertEquals(1000, stats.getMaxSize());
        }

        @Test
        @DisplayName("Returns 0 hit rate when no queries")
        void returnsZeroHitRateWhenNoQueries() {
            when(cacheRepository.size()).thenReturn(0);
            when(cacheRepository.getHitCount()).thenReturn(0L);
            when(cacheRepository.getMissCount()).thenReturn(0L);

            CacheStatsResponse stats = cacheService.getStats();

            assertEquals(0.0, stats.getHitRate());
        }

        @Test
        @DisplayName("Calculates 100% hit rate when all hits")
        void calculates100PercentHitRate() {
            when(cacheRepository.size()).thenReturn(1);
            when(cacheRepository.getHitCount()).thenReturn(50L);
            when(cacheRepository.getMissCount()).thenReturn(0L);

            CacheStatsResponse stats = cacheService.getStats();

            assertEquals(100.0, stats.getHitRate());
        }
    }

    @Nested
    @DisplayName("getAllEntries()")
    class GetAllEntriesTests {

        @Test
        @DisplayName("Returns empty list when cache is empty")
        void returnsEmptyListWhenEmpty() {
            when(cacheRepository.getAll()).thenReturn(Map.of());

            List<CacheEntryResponse> entries = cacheService.getAllEntries();

            assertTrue(entries.isEmpty());
        }

        @Test
        @DisplayName("Returns DTOs for all cached entries")
        void returnsDtosForAllEntries() {
            CacheEntry entry = createEntry("example.com", 300);
            when(cacheRepository.getAll()).thenReturn(Map.of("example.com:1", entry));

            List<CacheEntryResponse> entries = cacheService.getAllEntries();

            assertEquals(1, entries.size());
            CacheEntryResponse dto = entries.getFirst();
            assertEquals("example.com", dto.getDomain());
            assertEquals("A", dto.getRecordType());
            assertEquals(0, dto.getResponseCode());
            assertFalse(dto.getRecords().isEmpty());
            assertEquals("10.0.0.1", dto.getRecords().getFirst());
        }
    }

    @Nested
    @DisplayName("lookupDomain()")
    class LookupDomainTests {

        @Test
        @DisplayName("Returns entry when domain is cached")
        void returnsEntryWhenCached() {
            CacheEntry entry = createEntry("found.com", 300);
            when(cacheRepository.get("found.com:1")).thenReturn(Optional.of(entry));

            Optional<CacheEntryResponse> result = cacheService.lookupDomain("found.com");

            assertTrue(result.isPresent());
            assertEquals("found.com", result.get().getDomain());
        }

        @Test
        @DisplayName("Returns empty when domain is not cached")
        void returnsEmptyWhenNotCached() {
            when(cacheRepository.get("missing.com:1")).thenReturn(Optional.empty());

            Optional<CacheEntryResponse> result = cacheService.lookupDomain("missing.com");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("clearCache()")
    class ClearCacheTests {

        @Test
        @DisplayName("Delegates to repository clear")
        void delegatesToRepositoryClear() {
            cacheService.clearCache();

            verify(cacheRepository, times(1)).clear();
        }
    }
}
