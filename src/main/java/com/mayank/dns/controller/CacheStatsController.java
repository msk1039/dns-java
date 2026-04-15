package com.mayank.dns.controller;

import com.mayank.dns.model.CacheEntryResponse;
import com.mayank.dns.model.CacheStatsResponse;
import com.mayank.dns.service.CacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for DNS cache management and statistics.
 */
@RestController
@RequestMapping("/api/cache")
public class CacheStatsController {

    private final CacheService cacheService;

    public CacheStatsController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * GET /api/cache/stats
     * Returns cache statistics: hits, misses, hit rate, total entries, max size.
     */
    @GetMapping("/stats")
    public ResponseEntity<CacheStatsResponse> getStats() {
        return ResponseEntity.ok(cacheService.getStats());
    }

    /**
     * GET /api/cache/entries
     * Returns all cached entries with their domain, record type, TTL remaining, and IPs.
     */
    @GetMapping("/entries")
    public ResponseEntity<List<CacheEntryResponse>> getEntries() {
        return ResponseEntity.ok(cacheService.getAllEntries());
    }

    /**
     * GET /api/cache/lookup/{domain}
     * Looks up a specific domain in the cache.
     */
    @GetMapping("/lookup/{domain}")
    public ResponseEntity<CacheEntryResponse> lookupDomain(@PathVariable String domain) {
        return cacheService.lookupDomain(domain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/cache
     * Clears the entire DNS cache.
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearCache() {
        cacheService.clearCache();
        return ResponseEntity.ok(Map.of("status", "Cache cleared successfully"));
    }
}
