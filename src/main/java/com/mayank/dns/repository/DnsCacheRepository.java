package com.mayank.dns.repository;

import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for DNS cache storage.
 * Implementations can use in-memory storage (ConcurrentHashMap) or external stores (Redis).
 *
 * The cache key is a composite of domain name + record type (e.g., "example.com:A").
 */
public interface DnsCacheRepository {

    /**
     * Retrieves a cache entry by key.
     * Returns empty if the key is not found or the entry has expired.
     */
    Optional<CacheEntry> get(String key);

    /**
     * Stores a cache entry.
     */
    void put(String key, CacheEntry entry);

    /**
     * Removes a specific cache entry.
     */
    void remove(String key);

    /**
     * Clears all cache entries.
     */
    void clear();

    /**
     * Returns all non-expired cache entries.
     */
    Map<String, CacheEntry> getAll();

    /**
     * Returns the current number of entries in the cache.
     */
    int size();

    /**
     * Returns the total number of cache hits.
     */
    long getHitCount();

    /**
     * Returns the total number of cache misses.
     */
    long getMissCount();

    /**
     * Removes all expired entries from the cache.
     * Called periodically by the cleanup scheduler.
     */
    int evictExpired();

    /**
     * Builds a cache key from domain name and record type.
     * Example: "example.com" + 1 → "example.com:A"
     */
    static String buildKey(String domain, int recordType) {
        return domain.toLowerCase() + ":" + recordType;
    }
}
