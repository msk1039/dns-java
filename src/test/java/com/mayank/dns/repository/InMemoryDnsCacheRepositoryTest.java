package com.mayank.dns.repository;

import com.mayank.dns.config.DnsServerConfig;
import com.mayank.dns.protocol.DnsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InMemoryDnsCacheRepository — cache operations, stats, eviction.
 */
class InMemoryDnsCacheRepositoryTest {

    private InMemoryDnsCacheRepository repository;

    @BeforeEach
    void setUp() {
        DnsServerConfig config = new DnsServerConfig();
        config.getCache().setMaxSize(5); // small max for testing
        repository = new InMemoryDnsCacheRepository(config);
    }

    private CacheEntry createEntry(long ttlSeconds, int responseCode) {
        DnsRecord record = new DnsRecord("example.com", 1, 1, ttlSeconds, new byte[]{1, 2, 3, 4});
        return new CacheEntry(
                List.of(record),
                Instant.now().plusSeconds(ttlSeconds),
                responseCode
        );
    }

    private CacheEntry createExpiredEntry() {
        DnsRecord record = new DnsRecord("expired.com", 1, 1, 0, new byte[]{1, 2, 3, 4});
        return new CacheEntry(
                List.of(record),
                Instant.now().minusSeconds(10),
                0
        );
    }

    @Nested
    @DisplayName("get()")
    class GetTests {

        @Test
        @DisplayName("Returns empty for missing key")
        void returnsEmptyForMissingKey() {
            Optional<CacheEntry> result = repository.get("missing:1");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Returns entry for existing key")
        void returnsEntryForExistingKey() {
            repository.put("example.com:1", createEntry(300, 0));

            Optional<CacheEntry> result = repository.get("example.com:1");

            assertTrue(result.isPresent());
            assertEquals(0, result.get().getResponseCode());
        }

        @Test
        @DisplayName("Returns empty and removes expired entry")
        void returnsEmptyForExpiredEntry() {
            repository.put("expired.com:1", createExpiredEntry());

            Optional<CacheEntry> result = repository.get("expired.com:1");

            assertTrue(result.isEmpty());
            assertEquals(0, repository.size()); // entry should be removed
        }
    }

    @Nested
    @DisplayName("Cache statistics")
    class StatisticsTests {

        @Test
        @DisplayName("Hit count increments on cache hit")
        void hitCountIncrements() {
            repository.put("hit.com:1", createEntry(300, 0));

            repository.get("hit.com:1");
            repository.get("hit.com:1");

            assertEquals(2, repository.getHitCount());
            assertEquals(0, repository.getMissCount());
        }

        @Test
        @DisplayName("Miss count increments on cache miss")
        void missCountIncrements() {
            repository.get("miss1.com:1");
            repository.get("miss2.com:1");

            assertEquals(0, repository.getHitCount());
            assertEquals(2, repository.getMissCount());
        }

        @Test
        @DisplayName("Miss count increments when entry is expired")
        void missCountIncrementsOnExpired() {
            repository.put("expired.com:1", createExpiredEntry());

            repository.get("expired.com:1");

            assertEquals(0, repository.getHitCount());
            assertEquals(1, repository.getMissCount());
        }

        @Test
        @DisplayName("Initial hit and miss counts are zero")
        void initialCountsAreZero() {
            assertEquals(0, repository.getHitCount());
            assertEquals(0, repository.getMissCount());
        }
    }

    @Nested
    @DisplayName("put()")
    class PutTests {

        @Test
        @DisplayName("Adds entry and increases size")
        void addsEntryAndIncreasesSize() {
            repository.put("a.com:1", createEntry(300, 0));
            assertEquals(1, repository.size());
        }

        @Test
        @DisplayName("Overwrites existing entry with same key")
        void overwritesExistingEntry() {
            repository.put("a.com:1", createEntry(300, 0));
            repository.put("a.com:1", createEntry(600, 0));

            assertEquals(1, repository.size());
            Optional<CacheEntry> result = repository.get("a.com:1");
            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("Enforces max size — evicts expired first")
        void enforcesMaxSizeEvictsExpired() {
            // Fill cache to max (5)
            for (int i = 0; i < 5; i++) {
                repository.put("domain" + i + ".com:1", createEntry(300, 0));
            }
            assertEquals(5, repository.size());

            // Replace one entry with expired
            repository.put("domain0.com:1", createExpiredEntry());

            // Now put a new entry — should evict expired and succeed
            repository.put("new.com:1", createEntry(300, 0));

            assertTrue(repository.size() <= 5);
        }

        @Test
        @DisplayName("Refuses to add when cache is full and no expired entries")
        void refusesToAddWhenFullNoExpired() {
            // Fill cache to max (5) with non-expired entries
            for (int i = 0; i < 5; i++) {
                repository.put("domain" + i + ".com:1", createEntry(3600, 0));
            }

            // Try to add one more
            repository.put("overflow.com:1", createEntry(300, 0));

            // overflow.com should NOT be in cache
            assertEquals(5, repository.size());
            assertTrue(repository.get("overflow.com:1").isEmpty());
        }
    }

    @Nested
    @DisplayName("remove()")
    class RemoveTests {

        @Test
        @DisplayName("Removes existing entry")
        void removesExistingEntry() {
            repository.put("remove.com:1", createEntry(300, 0));
            repository.remove("remove.com:1");

            assertEquals(0, repository.size());
        }

        @Test
        @DisplayName("No-op for missing key")
        void noOpForMissingKey() {
            repository.remove("nonexistent:1");
            assertEquals(0, repository.size());
        }
    }

    @Nested
    @DisplayName("clear()")
    class ClearTests {

        @Test
        @DisplayName("Removes all entries")
        void removesAllEntries() {
            repository.put("a.com:1", createEntry(300, 0));
            repository.put("b.com:1", createEntry(300, 0));

            repository.clear();

            assertEquals(0, repository.size());
        }
    }

    @Nested
    @DisplayName("getAll()")
    class GetAllTests {

        @Test
        @DisplayName("Returns only non-expired entries")
        void returnsOnlyNonExpired() {
            repository.put("valid.com:1", createEntry(300, 0));
            repository.put("expired.com:1", createExpiredEntry());

            Map<String, CacheEntry> all = repository.getAll();

            assertEquals(1, all.size());
            assertTrue(all.containsKey("valid.com:1"));
            assertFalse(all.containsKey("expired.com:1"));
        }

        @Test
        @DisplayName("Returns empty map when cache is empty")
        void returnsEmptyMapWhenEmpty() {
            Map<String, CacheEntry> all = repository.getAll();
            assertTrue(all.isEmpty());
        }
    }

    @Nested
    @DisplayName("evictExpired()")
    class EvictExpiredTests {

        @Test
        @DisplayName("Removes expired entries and returns count")
        void removesExpiredEntries() {
            repository.put("valid.com:1", createEntry(300, 0));
            repository.put("exp1.com:1", createExpiredEntry());
            repository.put("exp2.com:1", createExpiredEntry());

            int evicted = repository.evictExpired();

            assertEquals(2, evicted);
            assertEquals(1, repository.size());
        }

        @Test
        @DisplayName("Returns 0 when no expired entries")
        void returnsZeroWhenNoExpired() {
            repository.put("valid.com:1", createEntry(300, 0));

            int evicted = repository.evictExpired();

            assertEquals(0, evicted);
            assertEquals(1, repository.size());
        }

        @Test
        @DisplayName("Returns 0 on empty cache")
        void returnsZeroOnEmptyCache() {
            assertEquals(0, repository.evictExpired());
        }
    }

    @Nested
    @DisplayName("buildKey()")
    class BuildKeyTests {

        @Test
        @DisplayName("Builds key with domain and record type")
        void buildsKeyCorrectly() {
            assertEquals("example.com:1", DnsCacheRepository.buildKey("example.com", 1));
        }

        @Test
        @DisplayName("Lowercases domain name")
        void lowercasesDomain() {
            assertEquals("example.com:1", DnsCacheRepository.buildKey("EXAMPLE.COM", 1));
        }

        @Test
        @DisplayName("Handles AAAA record type")
        void handlesAAAAType() {
            assertEquals("example.com:28", DnsCacheRepository.buildKey("example.com", 28));
        }
    }
}
