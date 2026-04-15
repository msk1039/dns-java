package com.mayank.dns.service;

import com.mayank.dns.protocol.DnsConstants;
import com.mayank.dns.protocol.DnsMessage;
import com.mayank.dns.protocol.DnsQuestion;
import com.mayank.dns.protocol.DnsRecord;
import com.mayank.dns.repository.CacheEntry;
import com.mayank.dns.repository.DnsCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DnsQueryService.
 * Uses Mockito to mock DnsCacheRepository and UpstreamResolverService.
 */
@ExtendWith(MockitoExtension.class)
class DnsQueryServiceTest {

    @Mock
    private DnsCacheRepository cacheRepository;

    @Mock
    private UpstreamResolverService upstreamResolver;

    private DnsQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new DnsQueryService(cacheRepository, upstreamResolver);
    }

    /**
     * Helper: create a standard DNS query for domain with A record type.
     */
    private DnsMessage createQuery(String domain) {
        DnsMessage query = new DnsMessage();
        query.getHeader().setId(1234);
        query.getHeader().setQr(DnsConstants.QR_QUERY);
        query.getHeader().setRd(true);
        query.getHeader().setQdCount(1);
        query.getQuestions().add(new DnsQuestion(domain, DnsConstants.RecordType.A.getValue(), DnsConstants.CLASS_IN));
        return query;
    }

    /**
     * Helper: create a CacheEntry with given records and TTL.
     */
    private CacheEntry createCacheEntry(List<DnsRecord> records, long ttlSeconds, int responseCode) {
        return new CacheEntry(records, Instant.now().plusSeconds(ttlSeconds), responseCode);
    }

    /**
     * Helper: create a mock upstream DNS response.
     */
    private DnsMessage createUpstreamResponse(DnsMessage query, DnsConstants.ResponseCode rcode, List<DnsRecord> answers) {
        return DnsMessage.buildResponse(query, rcode, answers);
    }

    @Nested
    @DisplayName("Empty questions")
    class EmptyQuestionsTests {

        @Test
        @DisplayName("Returns SERVFAIL for query with no questions")
        void returnsServfailForEmptyQuestions() {
            DnsMessage query = new DnsMessage();
            query.getHeader().setId(1234);
            query.getHeader().setQdCount(0);

            DnsMessage response = queryService.handleQuery(query);

            assertEquals(DnsConstants.ResponseCode.SERVFAIL.getValue(), response.getHeader().getRcode());
            verifyNoInteractions(cacheRepository);
            verifyNoInteractions(upstreamResolver);
        }
    }

    @Nested
    @DisplayName("Cache hit path")
    class CacheHitTests {

        @Test
        @DisplayName("Returns cached response on cache hit")
        void returnsCachedResponse() {
            DnsMessage query = createQuery("cached.com");
            DnsRecord cachedRecord = new DnsRecord("cached.com", 1, 1, 300, new byte[]{10, 0, 0, 1});
            CacheEntry cacheEntry = createCacheEntry(List.of(cachedRecord), 200, 0);

            when(cacheRepository.get("cached.com:1")).thenReturn(Optional.of(cacheEntry));

            DnsMessage response = queryService.handleQuery(query);

            assertEquals(DnsConstants.ResponseCode.NOERROR.getValue(), response.getHeader().getRcode());
            assertFalse(response.getAnswers().isEmpty());
            assertEquals("10.0.0.1", response.getAnswers().getFirst().getRdataAsString());

            // Should NOT call upstream
            verifyNoInteractions(upstreamResolver);
        }

        @Test
        @DisplayName("Returns cached NXDOMAIN on cache hit")
        void returnsCachedNxdomain() {
            DnsMessage query = createQuery("nxdomain.com");
            CacheEntry nxEntry = createCacheEntry(List.of(), 200, DnsConstants.ResponseCode.NXDOMAIN.getValue());

            when(cacheRepository.get("nxdomain.com:1")).thenReturn(Optional.of(nxEntry));

            DnsMessage response = queryService.handleQuery(query);

            assertEquals(DnsConstants.ResponseCode.NXDOMAIN.getValue(), response.getHeader().getRcode());
            assertTrue(response.getAnswers().isEmpty());
            verifyNoInteractions(upstreamResolver);
        }

        @Test
        @DisplayName("Adjusts TTLs based on remaining cache time")
        void adjustsTtlsFromCache() {
            DnsMessage query = createQuery("ttl.com");
            DnsRecord record = new DnsRecord("ttl.com", 1, 1, 300, new byte[]{10, 0, 0, 1});
            // Cache entry with 150 seconds remaining
            CacheEntry entry = createCacheEntry(List.of(record), 150, 0);

            when(cacheRepository.get("ttl.com:1")).thenReturn(Optional.of(entry));

            DnsMessage response = queryService.handleQuery(query);

            // TTL should be adjusted to remaining time (approximately 150)
            long answerTtl = response.getAnswers().getFirst().getTtl();
            assertTrue(answerTtl > 0 && answerTtl <= 150,
                    "Expected TTL between 1 and 150, got: " + answerTtl);
        }
    }

    @Nested
    @DisplayName("Cache miss → upstream path")
    class CacheMissTests {

        @Test
        @DisplayName("Forwards to upstream on cache miss and caches result")
        void forwardsToUpstreamAndCaches() {
            DnsMessage query = createQuery("upstream.com");
            DnsRecord answer = new DnsRecord("upstream.com", 1, 1, 300, new byte[]{8, 8, 8, 8});
            DnsMessage upstreamResponse = createUpstreamResponse(query, DnsConstants.ResponseCode.NOERROR, List.of(answer));

            when(cacheRepository.get("upstream.com:1")).thenReturn(Optional.empty());
            when(upstreamResolver.resolve(query)).thenReturn(upstreamResponse);

            DnsMessage response = queryService.handleQuery(query);

            assertEquals(DnsConstants.ResponseCode.NOERROR.getValue(), response.getHeader().getRcode());
            assertEquals(1, response.getAnswers().size());
            assertEquals("8.8.8.8", response.getAnswers().getFirst().getRdataAsString());

            // Verify cache was updated
            verify(cacheRepository).put(eq("upstream.com:1"), any(CacheEntry.class));
        }

        @Test
        @DisplayName("Caches NXDOMAIN response from upstream")
        void cachesNxdomainFromUpstream() {
            DnsMessage query = createQuery("nonexistent.com");
            DnsMessage upstreamResponse = DnsMessage.buildNxdomainResponse(query);

            when(cacheRepository.get("nonexistent.com:1")).thenReturn(Optional.empty());
            when(upstreamResolver.resolve(query)).thenReturn(upstreamResponse);

            DnsMessage response = queryService.handleQuery(query);

            assertEquals(DnsConstants.ResponseCode.NXDOMAIN.getValue(), response.getHeader().getRcode());

            // Verify NXDOMAIN was cached
            ArgumentCaptor<CacheEntry> captor = ArgumentCaptor.forClass(CacheEntry.class);
            verify(cacheRepository).put(eq("nonexistent.com:1"), captor.capture());
            assertEquals(DnsConstants.ResponseCode.NXDOMAIN.getValue(), captor.getValue().getResponseCode());
        }

        @Test
        @DisplayName("Caches with minimum TTL from multiple answers")
        void cachesWithMinimumTtl() {
            DnsMessage query = createQuery("multi.com");
            List<DnsRecord> answers = List.of(
                    new DnsRecord("multi.com", 1, 1, 200, new byte[]{1, 1, 1, 1}),
                    new DnsRecord("multi.com", 1, 1, 100, new byte[]{2, 2, 2, 2}),
                    new DnsRecord("multi.com", 1, 1, 300, new byte[]{3, 3, 3, 3})
            );
            DnsMessage upstreamResponse = createUpstreamResponse(query, DnsConstants.ResponseCode.NOERROR, answers);

            when(cacheRepository.get("multi.com:1")).thenReturn(Optional.empty());
            when(upstreamResolver.resolve(query)).thenReturn(upstreamResponse);

            queryService.handleQuery(query);

            ArgumentCaptor<CacheEntry> captor = ArgumentCaptor.forClass(CacheEntry.class);
            verify(cacheRepository).put(eq("multi.com:1"), captor.capture());

            // The cache entry should expire based on min TTL (100 seconds)
            CacheEntry cached = captor.getValue();
            long remaining = cached.getRemainingTtlSeconds();
            assertTrue(remaining > 0 && remaining <= 100,
                    "Expected remaining TTL ≤ 100, got: " + remaining);
        }
    }

    @Nested
    @DisplayName("Upstream errors")
    class UpstreamErrorTests {

        @Test
        @DisplayName("Returns SERVFAIL on upstream timeout")
        void returnsServfailOnTimeout() {
            DnsMessage query = createQuery("timeout.com");

            when(cacheRepository.get("timeout.com:1")).thenReturn(Optional.empty());
            when(upstreamResolver.resolve(query))
                    .thenThrow(new UpstreamResolverService.UpstreamTimeoutException("Timeout", new Exception()));

            DnsMessage response = queryService.handleQuery(query);

            assertEquals(DnsConstants.ResponseCode.SERVFAIL.getValue(), response.getHeader().getRcode());
            verify(cacheRepository, never()).put(anyString(), any(CacheEntry.class));
        }

        @Test
        @DisplayName("Returns SERVFAIL on upstream resolve error")
        void returnsServfailOnResolveError() {
            DnsMessage query = createQuery("error.com");

            when(cacheRepository.get("error.com:1")).thenReturn(Optional.empty());
            when(upstreamResolver.resolve(query))
                    .thenThrow(new UpstreamResolverService.UpstreamResolveException("Error", new Exception()));

            DnsMessage response = queryService.handleQuery(query);

            assertEquals(DnsConstants.ResponseCode.SERVFAIL.getValue(), response.getHeader().getRcode());
            verify(cacheRepository, never()).put(anyString(), any(CacheEntry.class));
        }
    }

    @Nested
    @DisplayName("Response structure")
    class ResponseStructureTests {

        @Test
        @DisplayName("Response preserves query ID")
        void responsePreservesQueryId() {
            DnsMessage query = createQuery("id.com");
            DnsRecord answer = new DnsRecord("id.com", 1, 1, 300, new byte[]{1, 2, 3, 4});
            DnsMessage upstreamResponse = createUpstreamResponse(query, DnsConstants.ResponseCode.NOERROR, List.of(answer));

            when(cacheRepository.get("id.com:1")).thenReturn(Optional.empty());
            when(upstreamResolver.resolve(query)).thenReturn(upstreamResponse);

            DnsMessage response = queryService.handleQuery(query);

            assertEquals(1234, response.getHeader().getId());
        }

        @Test
        @DisplayName("Response has QR flag set to response")
        void responseHasQrResponseFlag() {
            DnsMessage query = createQuery("qr.com");
            DnsRecord answer = new DnsRecord("qr.com", 1, 1, 300, new byte[]{1, 2, 3, 4});
            DnsMessage upstreamResponse = createUpstreamResponse(query, DnsConstants.ResponseCode.NOERROR, List.of(answer));

            when(cacheRepository.get("qr.com:1")).thenReturn(Optional.empty());
            when(upstreamResolver.resolve(query)).thenReturn(upstreamResponse);

            DnsMessage response = queryService.handleQuery(query);

            assertEquals(DnsConstants.QR_RESPONSE, response.getHeader().getQr());
        }
    }
}
