package com.mayank.dns.controller;

import com.mayank.dns.model.CacheEntryResponse;
import com.mayank.dns.model.CacheStatsResponse;
import com.mayank.dns.service.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for CacheStatsController.
 * Tests all REST API endpoints for cache management.
 */
@WebMvcTest(CacheStatsController.class)
class CacheStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CacheService cacheService;

    @Test
    @DisplayName("GET /api/cache/stats returns 200 with stats JSON")
    void getStatsReturns200WithJson() throws Exception {
        CacheStatsResponse stats = new CacheStatsResponse(10, 80, 20, 80.0, 1000);
        when(cacheService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries").value(10))
                .andExpect(jsonPath("$.hits").value(80))
                .andExpect(jsonPath("$.misses").value(20))
                .andExpect(jsonPath("$.hitRate").value(80.0))
                .andExpect(jsonPath("$.maxSize").value(1000));
    }

    @Test
    @DisplayName("GET /api/cache/stats returns 200 with zero stats")
    void getStatsReturnsZeroStats() throws Exception {
        CacheStatsResponse stats = new CacheStatsResponse(0, 0, 0, 0.0, 1000);
        when(cacheService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries").value(0))
                .andExpect(jsonPath("$.hits").value(0))
                .andExpect(jsonPath("$.hitRate").value(0.0));
    }

    @Test
    @DisplayName("GET /api/cache/entries returns 200 with entry list")
    void getEntriesReturns200WithList() throws Exception {
        CacheEntryResponse entry = new CacheEntryResponse(
                "google.com", "A", 0, 250, List.of("142.250.80.46")
        );
        when(cacheService.getAllEntries()).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/cache/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].domain").value("google.com"))
                .andExpect(jsonPath("$[0].recordType").value("A"))
                .andExpect(jsonPath("$[0].responseCode").value(0))
                .andExpect(jsonPath("$[0].ttlRemainingSeconds").value(250))
                .andExpect(jsonPath("$[0].records[0]").value("142.250.80.46"));
    }

    @Test
    @DisplayName("GET /api/cache/entries returns 200 with empty list")
    void getEntriesReturnsEmptyList() throws Exception {
        when(cacheService.getAllEntries()).thenReturn(List.of());

        mockMvc.perform(get("/api/cache/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/cache/entries returns entry with multiple IPs")
    void getEntriesReturnsMultipleIps() throws Exception {
        CacheEntryResponse entry = new CacheEntryResponse(
                "amazon.com", "A", 0, 60,
                List.of("98.87.170.74", "98.87.170.71", "98.82.161.185")
        );
        when(cacheService.getAllEntries()).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/cache/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].records.length()").value(3))
                .andExpect(jsonPath("$[0].records[0]").value("98.87.170.74"))
                .andExpect(jsonPath("$[0].records[2]").value("98.82.161.185"));
    }

    @Test
    @DisplayName("GET /api/cache/lookup/{domain} returns 200 when found")
    void lookupDomainReturns200WhenFound() throws Exception {
        CacheEntryResponse entry = new CacheEntryResponse(
                "github.com", "A", 0, 120, List.of("140.82.121.3")
        );
        when(cacheService.lookupDomain("github.com")).thenReturn(Optional.of(entry));

        mockMvc.perform(get("/api/cache/lookup/github.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domain").value("github.com"))
                .andExpect(jsonPath("$.records[0]").value("140.82.121.3"));
    }

    @Test
    @DisplayName("GET /api/cache/lookup/{domain} returns 404 when not found")
    void lookupDomainReturns404WhenNotFound() throws Exception {
        when(cacheService.lookupDomain("unknown.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/cache/lookup/unknown.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/cache returns 200 with success message")
    void clearCacheReturns200() throws Exception {
        doNothing().when(cacheService).clearCache();

        mockMvc.perform(delete("/api/cache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Cache cleared successfully"));

        verify(cacheService, times(1)).clearCache();
    }

    @Test
    @DisplayName("GET /api/cache/lookup with NXDOMAIN cached entry")
    void lookupNxdomainCachedEntry() throws Exception {
        CacheEntryResponse nxEntry = new CacheEntryResponse(
                "nonexistent.com", "A", 3, 200, List.of()
        );
        when(cacheService.lookupDomain("nonexistent.com")).thenReturn(Optional.of(nxEntry));

        mockMvc.perform(get("/api/cache/lookup/nonexistent.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value(3))
                .andExpect(jsonPath("$.records").isEmpty());
    }
}
