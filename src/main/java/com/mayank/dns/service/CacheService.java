package com.mayank.dns.service;

import com.mayank.dns.config.DnsServerConfig;
import com.mayank.dns.model.CacheEntryResponse;
import com.mayank.dns.model.CacheStatsResponse;
import com.mayank.dns.protocol.DnsConstants;
import com.mayank.dns.protocol.DnsRecord;
import com.mayank.dns.repository.CacheEntry;
import com.mayank.dns.repository.DnsCacheRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service layer for cache management operations.
 * Translates between the repository layer and the REST API DTOs.
 */
@Service
public class CacheService {

    private final DnsCacheRepository cacheRepository;
    private final DnsServerConfig config;

    public CacheService(DnsCacheRepository cacheRepository, DnsServerConfig config) {
        this.cacheRepository = cacheRepository;
        this.config = config;
    }

    /**
     * Returns cache statistics: hits, misses, hit rate, entry count, max size.
     */
    public CacheStatsResponse getStats() {
        long hits = cacheRepository.getHitCount();
        long misses = cacheRepository.getMissCount();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total * 100.0 : 0.0;

        return new CacheStatsResponse(
                cacheRepository.size(),
                hits,
                misses,
                Math.round(hitRate * 100.0) / 100.0, // round to 2 decimal places
                config.getCache().getMaxSize()
        );
    }

    /**
     * Returns all cached entries as DTOs.
     */
    public List<CacheEntryResponse> getAllEntries() {
        Map<String, CacheEntry> entries = cacheRepository.getAll();
        List<CacheEntryResponse> result = new ArrayList<>();

        for (Map.Entry<String, CacheEntry> entry : entries.entrySet()) {
            result.add(toDto(entry.getKey(), entry.getValue()));
        }

        return result;
    }

    /**
     * Looks up a specific domain in the cache.
     * The domain is combined with A record type (1) for the lookup key.
     */
    public Optional<CacheEntryResponse> lookupDomain(String domain) {
        String key = DnsCacheRepository.buildKey(domain, DnsConstants.RecordType.A.getValue());
        return cacheRepository.get(key).map(entry -> toDto(key, entry));
    }

    /**
     * Clears the entire cache.
     */
    public void clearCache() {
        cacheRepository.clear();
    }

    /**
     * Converts a cache key + entry to a DTO for the REST API.
     */
    private CacheEntryResponse toDto(String key, CacheEntry entry) {
        // Parse key: "example.com:1" → domain="example.com", type=1
        String[] parts = key.split(":");
        String domain = parts[0];
        int typeValue = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        String recordType = DnsConstants.RecordType.fromValue(typeValue).name();

        List<String> recordStrings = entry.getRecords().stream()
                .map(DnsRecord::getRdataAsString)
                .toList();

        return new CacheEntryResponse(
                domain,
                recordType,
                entry.getResponseCode(),
                entry.getRemainingTtlSeconds(),
                recordStrings
        );
    }
}
