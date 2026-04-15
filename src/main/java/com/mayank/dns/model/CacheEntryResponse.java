package com.mayank.dns.model;

import java.util.List;

/**
 * DTO for a single cache entry returned by the REST API.
 */
public class CacheEntryResponse {

    private String domain;
    private String recordType;
    private int responseCode;
    private long ttlRemainingSeconds;
    private List<String> records;

    public CacheEntryResponse() {
    }

    public CacheEntryResponse(String domain, String recordType, int responseCode,
                               long ttlRemainingSeconds, List<String> records) {
        this.domain = domain;
        this.recordType = recordType;
        this.responseCode = responseCode;
        this.ttlRemainingSeconds = ttlRemainingSeconds;
        this.records = records;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public long getTtlRemainingSeconds() {
        return ttlRemainingSeconds;
    }

    public void setTtlRemainingSeconds(long ttlRemainingSeconds) {
        this.ttlRemainingSeconds = ttlRemainingSeconds;
    }

    public List<String> getRecords() {
        return records;
    }

    public void setRecords(List<String> records) {
        this.records = records;
    }
}
