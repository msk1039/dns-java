package com.mayank.dns.model;

/**
 * DTO for cache statistics returned by the REST API.
 */
public class CacheStatsResponse {

    private int totalEntries;
    private long hits;
    private long misses;
    private double hitRate;
    private int maxSize;

    public CacheStatsResponse() {
    }

    public CacheStatsResponse(int totalEntries, long hits, long misses, double hitRate, int maxSize) {
        this.totalEntries = totalEntries;
        this.hits = hits;
        this.misses = misses;
        this.hitRate = hitRate;
        this.maxSize = maxSize;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    public long getMisses() {
        return misses;
    }

    public void setMisses(long misses) {
        this.misses = misses;
    }

    public double getHitRate() {
        return hitRate;
    }

    public void setHitRate(double hitRate) {
        this.hitRate = hitRate;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }
}
