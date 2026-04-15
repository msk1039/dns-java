# 🌐 Custom DNS Server

A fully custom DNS server built from scratch in **Java 26** with **Spring Boot 4.0.5** — no third-party DNS libraries used. Parses and constructs raw DNS protocol bytes per [RFC 1035](https://datatracker.ietf.org/doc/html/rfc1035).

## Features

| Feature | Description |
|---|---|
| **DNS Resolution (A records)** | Resolves domain names to IPv4 addresses via UDP |
| **Multiple A Records** | Returns all IPs when a domain has multiple A records |
| **Caching with TTL** | Caches DNS responses with automatic TTL-based expiration |
| **Negative Caching (NXDOMAIN)** | Caches non-existent domain responses for 300 seconds |
| **Timeout Handling** | Returns SERVFAIL if upstream DNS doesn't respond within the configured timeout |
| **Cache Stats REST API** | HTTP endpoints to view stats, entries, lookups, and clear cache |
| **Scheduled Cleanup** | Background task periodically evicts expired cache entries |
| **Virtual Threads** | Each DNS query handled on a Java 21+ virtual thread for high concurrency |
| **Modular Architecture** | 3-layer design (Controller → Service → Repository) — swap in Redis by implementing one interface |

## Tech Stack

- **Java 26** (Oracle)
- **Spring Boot 4.0.5**
- **UDP DatagramSocket** (DNS transport)
- **ConcurrentHashMap** (in-memory cache)
- No external DNS libraries

---

## Quick Start

### Prerequisites

- Java 26 (or 21+)
- Maven (wrapper included)

### Run

```bash
./mvnw spring-boot:run
```

The server starts two listeners:
- **DNS UDP Server** on port `5354`
- **REST API** (Tomcat) on port `8080`

### Test with `dig`

```bash
# Resolve a domain
dig @127.0.0.1 -p 5354 google.com A

# Short output
dig @127.0.0.1 -p 5354 github.com A +short

# Test NXDOMAIN
dig @127.0.0.1 -p 5354 thisdomaindoesnotexist.com A

# Multiple A records
dig @127.0.0.1 -p 5354 amazon.com A +short
```

### Test with `nslookup`

```bash
nslookup -port=5354 google.com 127.0.0.1
```

---

## REST API

Base URL: `http://localhost:8080/api/cache`

### `GET /api/cache/stats`

Returns cache hit/miss statistics.

```bash
curl http://localhost:8080/api/cache/stats
```

**Response:**
```json
{
  "totalEntries": 3,
  "hits": 10,
  "misses": 5,
  "hitRate": 66.67,
  "maxSize": 1000
}
```

### `GET /api/cache/entries`

Lists all cached domains with their records and remaining TTL.

```bash
curl http://localhost:8080/api/cache/entries
```

**Response:**
```json
[
  {
    "domain": "google.com",
    "recordType": "A",
    "responseCode": 0,
    "ttlRemainingSeconds": 245,
    "records": ["216.58.203.46"]
  },
  {
    "domain": "amazon.com",
    "recordType": "A",
    "responseCode": 0,
    "ttlRemainingSeconds": 180,
    "records": ["98.87.170.74", "98.87.170.71", "98.82.161.185"]
  }
]
```

### `GET /api/cache/lookup/{domain}`

Look up a specific domain in the cache.

```bash
curl http://localhost:8080/api/cache/lookup/google.com
```

**Response (200 OK):**
```json
{
  "domain": "google.com",
  "recordType": "A",
  "responseCode": 0,
  "ttlRemainingSeconds": 120,
  "records": ["216.58.203.46"]
}
```

**Response (404 Not Found):** if domain is not in cache.

### `DELETE /api/cache`

Clears the entire DNS cache.

```bash
curl -X DELETE http://localhost:8080/api/cache
```

**Response:**
```json
{
  "status": "Cache cleared successfully"
}
```

---

## Configuration

All settings are in `src/main/resources/application.properties`:

```properties
# DNS Server
dns.server.port=5354                     # UDP port for DNS queries
dns.server.upstream=8.8.8.8              # Upstream DNS server
dns.server.upstream-port=53              # Upstream DNS port
dns.server.timeout-ms=3000               # Upstream query timeout (ms)

# Cache
dns.cache.max-size=1000                  # Maximum cache entries
dns.cache.cleanup-interval-seconds=60    # TTL cleanup interval (seconds)
```

---

## Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                      Spring Boot Application                    │
│                                                                  │
│  ┌─ CONTROLLER LAYER ─────────────────────────────────────────┐ │
│  │  DnsUdpServer          ← UDP :5354 (DNS queries)           │ │
│  │  CacheStatsController  ← HTTP :8080 (REST API)             │ │
│  └────────────────────────────┬────────────────────────────────┘ │
│                               │                                   │
│  ┌─ SERVICE LAYER ────────────▼───────────────────────────────┐ │
│  │  DnsQueryService        ← Orchestrates resolution          │ │
│  │  CacheService           ← Cache business logic for REST    │ │
│  │  UpstreamResolverService← Forwards to upstream DNS         │ │
│  └────────────────────────────┬────────────────────────────────┘ │
│                               │                                   │
│  ┌─ REPOSITORY LAYER ────────▼───────────────────────────────┐ │
│  │  DnsCacheRepository (interface)                             │ │
│  │    └─ InMemoryDnsCacheRepository (ConcurrentHashMap)        │ │
│  │    └─ (RedisDnsCacheRepository — future)                    │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌─ PROTOCOL LAYER (pure DNS wire format) ────────────────────┐ │
│  │  DnsMessage, DnsHeader, DnsQuestion, DnsRecord              │ │
│  │  DnsMessageDecoder (bytes → objects)                        │ │
│  │  DnsMessageEncoder (objects → bytes)                        │ │
│  │  DnsConstants (RecordType, ResponseCode enums)              │ │
│  └─────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
```

### Query Flow

```
Client (dig/nslookup)
    │
    ▼ UDP packet
DnsUdpServer
    │ decode bytes → DnsMessage
    ▼
DnsQueryService
    │
    ├─ Check cache (DnsCacheRepository)
    │   ├─ HIT → return cached records (with adjusted TTL)
    │   └─ MISS ↓
    │
    ├─ Forward to upstream (UpstreamResolverService)
    │   ├─ Success → cache result → return response
    │   ├─ NXDOMAIN → cache negative → return NXDOMAIN
    │   └─ Timeout → return SERVFAIL
    │
    ▼ encode DnsMessage → bytes
DnsUdpServer → send UDP response to client
```

---

## Project Structure

```
com.mayank.dns/
├── DnsApplication.java                     # Entry point + @EnableScheduling
│
├── config/
│   └── DnsServerConfig.java               # @ConfigurationProperties (dns.*)
│
├── protocol/                               # Pure DNS wire format (RFC 1035)
│   ├── DnsConstants.java                   # RecordType & ResponseCode enums
│   ├── DnsHeader.java                      # 12-byte DNS header
│   ├── DnsQuestion.java                    # Question section
│   ├── DnsRecord.java                      # Resource record (name, type, TTL, rdata)
│   ├── DnsMessage.java                     # Full message + factory methods
│   ├── DnsMessageDecoder.java             # byte[] → DnsMessage (handles compression)
│   └── DnsMessageEncoder.java             # DnsMessage → byte[]
│
├── repository/                             # Data access layer
│   ├── DnsCacheRepository.java            # Interface (swap to Redis easily)
│   ├── CacheEntry.java                    # Records + expiry + response code
│   └── InMemoryDnsCacheRepository.java    # ConcurrentHashMap + scheduled cleanup
│
├── service/                                # Business logic
│   ├── DnsQueryService.java               # Orchestrator: cache → upstream → cache → respond
│   ├── CacheService.java                  # Cache stats/management for REST API
│   └── UpstreamResolverService.java       # UDP forwarding to upstream DNS
│
├── controller/                             # Entry points
│   ├── DnsUdpServer.java                  # UDP server (virtual threads)
│   └── CacheStatsController.java          # REST API controller
│
└── model/                                  # DTOs
    ├── CacheStatsResponse.java            # Stats DTO
    └── CacheEntryResponse.java            # Cache entry DTO
```

