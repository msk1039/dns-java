package com.mayank.dns.service;

import com.mayank.dns.protocol.DnsConstants;
import com.mayank.dns.protocol.DnsMessage;
import com.mayank.dns.protocol.DnsQuestion;
import com.mayank.dns.protocol.DnsRecord;
import com.mayank.dns.repository.CacheEntry;
import com.mayank.dns.repository.DnsCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrator service for DNS query resolution.
 * Flow: check cache → resolve upstream → cache result → build response.
 */
@Service
public class DnsQueryService {

    private static final Logger log = LoggerFactory.getLogger(DnsQueryService.class);

    /** Default TTL for NXDOMAIN negative cache entries (seconds). */
    private static final long NXDOMAIN_CACHE_TTL = 300;

    private final DnsCacheRepository cacheRepository;
    private final UpstreamResolverService upstreamResolver;

    public DnsQueryService(DnsCacheRepository cacheRepository, UpstreamResolverService upstreamResolver) {
        this.cacheRepository = cacheRepository;
        this.upstreamResolver = upstreamResolver;
    }

    /**
     * Handles an incoming DNS query message and returns a response message.
     * 
     * 1. Extracts the first question from the query
     * 2. Checks the cache for a cached response
     * 3. If cache miss, forwards to upstream DNS
     * 4. Caches the result (including NXDOMAIN)
     * 5. Builds and returns the DNS response
     */
    public DnsMessage handleQuery(DnsMessage query) {
        if (query.getQuestions().isEmpty()) {
            log.warn("Received query with no questions");
            return DnsMessage.buildServfailResponse(query);
        }

        DnsQuestion question = query.getQuestions().getFirst();
        String domain = question.getName();
        int recordType = question.getType();
        String cacheKey = DnsCacheRepository.buildKey(domain, recordType);

        log.info("Query: {} {} (key={})", domain, DnsConstants.RecordType.fromValue(recordType), cacheKey);

        // --- Step 1: Check cache ---
        Optional<CacheEntry> cached = cacheRepository.get(cacheKey);
        if (cached.isPresent()) {
            CacheEntry entry = cached.get();
            log.info("Cache HIT for {} (ttl remaining: {}s)", cacheKey, entry.getRemainingTtlSeconds());

            DnsConstants.ResponseCode rcode = DnsConstants.ResponseCode.fromValue(entry.getResponseCode());
            List<DnsRecord> records = adjustTtls(entry.getRecords(), entry.getRemainingTtlSeconds());
            return DnsMessage.buildResponse(query, rcode, records);
        }

        log.info("Cache MISS for {}", cacheKey);

        // --- Step 2: Forward to upstream ---
        DnsMessage upstreamResponse;
        try {
            upstreamResponse = upstreamResolver.resolve(query);
        } catch (UpstreamResolverService.UpstreamTimeoutException e) {
            log.warn("Upstream timeout for {}", cacheKey);
            return DnsMessage.buildServfailResponse(query);
        } catch (UpstreamResolverService.UpstreamResolveException e) {
            log.error("Upstream resolve error for {}: {}", cacheKey, e.getMessage());
            return DnsMessage.buildServfailResponse(query);
        }

        // --- Step 3: Cache the result ---
        cacheResponse(cacheKey, upstreamResponse);

        // --- Step 4: Return the upstream response ---
        log.info("Resolved {} → rcode={}, answers={}",
                cacheKey,
                DnsConstants.ResponseCode.fromValue(upstreamResponse.getHeader().getRcode()),
                upstreamResponse.getAnswers().size());

        return upstreamResponse;
    }

    /**
     * Caches the upstream DNS response.
     * For NXDOMAIN, caches with a fixed negative TTL.
     * For successful responses, caches using the minimum TTL from the answer records.
     */
    private void cacheResponse(String cacheKey, DnsMessage response) {
        int rcode = response.getHeader().getRcode();

        if (rcode == DnsConstants.ResponseCode.NXDOMAIN.getValue()) {
            // Negative caching for NXDOMAIN
            Instant expiresAt = Instant.now().plusSeconds(NXDOMAIN_CACHE_TTL);
            CacheEntry entry = new CacheEntry(new ArrayList<>(), expiresAt, rcode);
            cacheRepository.put(cacheKey, entry);
            log.debug("Cached NXDOMAIN for {} (ttl={}s)", cacheKey, NXDOMAIN_CACHE_TTL);
            return;
        }

        if (rcode == DnsConstants.ResponseCode.NOERROR.getValue() && !response.getAnswers().isEmpty()) {
            // Find the minimum TTL from all answer records
            long minTtl = response.getAnswers().stream()
                    .mapToLong(DnsRecord::getTtl)
                    .min()
                    .orElse(60);

            // Ensure at least 1 second TTL
            minTtl = Math.max(1, minTtl);

            Instant expiresAt = Instant.now().plusSeconds(minTtl);
            CacheEntry entry = new CacheEntry(new ArrayList<>(response.getAnswers()), expiresAt, rcode);
            cacheRepository.put(cacheKey, entry);
            log.debug("Cached {} records for {} (ttl={}s)", response.getAnswers().size(), cacheKey, minTtl);
        }
    }

    /**
     * Adjusts the TTL of cached records to reflect the remaining cache time.
     * This ensures clients see an accurate TTL countdown.
     */
    private List<DnsRecord> adjustTtls(List<DnsRecord> records, long remainingTtl) {
        List<DnsRecord> adjusted = new ArrayList<>();
        for (DnsRecord record : records) {
            DnsRecord copy = new DnsRecord(
                    record.getName(),
                    record.getType(),
                    record.getDnsClass(),
                    remainingTtl,
                    record.getRdata()
            );
            adjusted.add(copy);
        }
        return adjusted;
    }
}
