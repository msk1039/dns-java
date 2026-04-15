package com.mayank.dns.repository;

import com.mayank.dns.config.DnsServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of DnsCacheRepository using ConcurrentHashMap.
 * Tracks hit/miss statistics and periodically evicts expired entries.
 *
 * This can be swapped with a Redis-based implementation later by simply
 * creating a new @Repository class implementing DnsCacheRepository.
 */
@Repository
public class InMemoryDnsCacheRepository implements DnsCacheRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDnsCacheRepository.class);

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final DnsServerConfig config;

    public InMemoryDnsCacheRepository(DnsServerConfig config) {
        this.config = config;
    }

    @Override
    public Optional<CacheEntry> get(String key) {
        CacheEntry entry = cache.get(key);

        if (entry == null) {
            missCount.incrementAndGet();
            return Optional.empty();
        }

        if (entry.isExpired()) {
            cache.remove(key);
            missCount.incrementAndGet();
            log.debug("Cache entry expired and removed: {}", key);
            return Optional.empty();
        }

        hitCount.incrementAndGet();
        return Optional.of(entry);
    }

    @Override
    public void put(String key, CacheEntry entry) {
        // Enforce max size: if full, evict expired first, then skip if still full
        if (cache.size() >= config.getCache().getMaxSize()) {
            evictExpired();
            if (cache.size() >= config.getCache().getMaxSize()) {
                log.warn("Cache is full ({} entries), not caching: {}", cache.size(), key);
                return;
            }
        }
        cache.put(key, entry);
        log.debug("Cached: {} (expires at {})", key, entry.getExpiresAt());
    }

    @Override
    public void remove(String key) {
        cache.remove(key);
    }

    @Override
    public void clear() {
        cache.clear();
        log.info("Cache cleared");
    }

    @Override
    public Map<String, CacheEntry> getAll() {
        return cache.entrySet().stream()
                .filter(e -> !e.getValue().isExpired())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public long getHitCount() {
        return hitCount.get();
    }

    @Override
    public long getMissCount() {
        return missCount.get();
    }

    @Override
    @Scheduled(fixedDelayString = "${dns.cache.cleanup-interval-seconds:60}000")
    public int evictExpired() {
        int evicted = 0;
        var iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                evicted++;
            }
        }
        if (evicted > 0) {
            log.info("Evicted {} expired cache entries. Remaining: {}", evicted, cache.size());
        }
        return evicted;
    }
}
